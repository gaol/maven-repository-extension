/*
 * JBoss, Home of Professional Open Source
 * Copyright @year, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.maven.extensions.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
class OrderedRepositoryConfiguration {

    private static final String DEFAULT_PROP_FILE = "config.properties";

    /**
     * Additional Configure URL, can be relative to current classpath, a local file or an URL points to a remote resource.
     */
    private final String additionalConfigURL;

    private static final String KEY_REGEX = "regex";
    private static final String KEY_REPOS = "repos";
    private static final String KEY_DESCRPTION = "description";
    private static final String DOT = ".";
    private static final String REGEX_PREFIX = KEY_REGEX + DOT;

    private static final String DEFAULT = "default";

    private static final String KEY_DEFAULT_REGEX_REPOS = REGEX_PREFIX + DEFAULT + DOT + KEY_REPOS;
    private String[] defaultRepos; // only repository id string list

    private static final String KEY_REPO_INCLUDED = "repos.included";
    private String[] includeRepos = new String[0];

    private List<OrderRule> orderRules = new ArrayList<OrderRule>(0);

    private boolean includeReposDefault;

    static class OrderRule {
        private int index;
        private String pattern;
        private String description;
        private String[] repos;// defined order matters
        private boolean includeOtherRepo;

        public String getPattern() {
            return pattern;
        }
        public String getDescription() {
            return description;
        }
        public String[] getRepos() {
            return repos;
        }
        public int getIndex() {
            return index;
        }
        public boolean isIncludeOtherRepo() {
            return includeOtherRepo;
        }
    }

    OrderedRepositoryConfiguration(String additionalConfigURL) {
        this.additionalConfigURL = additionalConfigURL;
        loadRules();
    }

    private void loadRules() {
        Properties defaultProp = null;
        try {
            defaultProp = loadDefaultConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Properties extraProp = null;
        try {
            extraProp = loadAdditionalConfig();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Properties props = mergeProperties(defaultProp, extraProp);
        if (props != null) {
            Map<String, OrderRule> rules = new HashMap<String, OrderRule>();
            for (Map.Entry<Object, Object> entry: props.entrySet()) {
                String key = entry.getKey().toString().trim();
                String value = entry.getValue().toString().trim();
                if (key.equals(KEY_DEFAULT_REGEX_REPOS)) {
                    defaultRepos = value.split(",");
                } else if (key.equals(KEY_REPO_INCLUDED)) {
                    includeRepos = value.split(",");
                    includeReposDefault = value.contains(REGEX_PREFIX + DEFAULT);
                } else if (key.startsWith(REGEX_PREFIX) && !key.equals(KEY_DEFAULT_REGEX_REPOS)) {
                    Integer number = Integer.valueOf(key.split("\\.")[1]);
                    OrderRule regexConfig = rules.get(REGEX_PREFIX + number);
                    if (regexConfig == null) {
                        regexConfig = new OrderRule();
                        regexConfig.index = number;
                        rules.put(REGEX_PREFIX + number, regexConfig);
                    }
                    if (key.endsWith(KEY_REPOS)) {
                        regexConfig.repos = value.split(",");
                    } else if (key.endsWith(KEY_DESCRPTION)) {
                        regexConfig.description = value;
                    } else {
                        regexConfig.pattern = value;
                    }
                }
            }
            orderRules = new ArrayList<OrderRule>(rules.values());
            Collections.sort(orderRules, new Comparator<OrderRule>() {
                @Override
                public int compare(OrderRule r1, OrderRule r2) {
                    return r1.index - r2.index;
                }
            });
            for (OrderRule rule: orderRules) {
                for (String ip: includeRepos) {
                    if (ip.trim().equals(REGEX_PREFIX + rule.getIndex())) {
                        rule.includeOtherRepo = true;
                    }
                }
            }
        }
    }

    private void debug(String message) {
        if (Boolean.getBoolean(OrderedRepositorySystem.DEBUG)) {
            System.out.println("[DEBUG] " + message);
        }
    }

    private Properties loadDefaultConfiguration() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROP_FILE)){
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                debug("Loaded Default Properties File: " + DEFAULT_PROP_FILE);
                return props;
            }
            return null;
        }
    }

    private Properties loadAdditionalConfig() throws IOException {
        if (additionalConfigURL != null && additionalConfigURL.trim().length() > 0) {
            // try it in current classpath
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(additionalConfigURL.trim())){
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    debug("Loaded Additional Properties from: " + additionalConfigURL);
                    return props;
                }
            }
            // try it as a local file
            File localFile = new File(additionalConfigURL.trim());
            if (localFile.exists() && localFile.isFile() && localFile.canRead()) {
                try (InputStream in = new FileInputStream(localFile)) {
                    Properties props = new Properties();
                    props.load(in);
                    debug("Loaded Additional Properties from local file: " + additionalConfigURL);
                    return props;
                }
            }
            // try it as an URL
            try (InputStream in = new URL(additionalConfigURL.trim()).openStream()){
                Properties props = new Properties();
                props.load(in);
                debug("Loaded Additional Properties from URL: " + additionalConfigURL);
                return props;
            }
        }
        return null;
    }

    private Properties mergeProperties(Properties defaultProp, Properties extraProp) {
        if (defaultProp != null && extraProp == null) {
            return defaultProp;
        }
        if (defaultProp == null && extraProp != null) {
            return extraProp;
        }
        if (defaultProp == null && extraProp == null) {
            return null;
        }
        // both are non-null
        Properties props = new Properties();
        for (Map.Entry<Object, Object> entry: defaultProp.entrySet()) {
            props.put(entry.getKey(), extraProp.getOrDefault(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<Object, Object> entry: extraProp.entrySet()) {
            props.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return props;
    }

    /**
     * @return All defined rules in order
     */
    List<OrderRule> getOrderedRules() {
        return orderRules;
    }

    /**
     * @return some repositories may be defined in sub module pom, we need to know what we need to append
     */
    String[] getIncludeRepoRules() {
        return includeRepos;
    }

    /**
     * Gets default repository order if no pattern is matched for the G:A:V
     */
    String[] getDefaultRepos() {
        return defaultRepos;
    }

    /**
     * @return whether includes other repos by default
     */
    boolean isReposIncludedByDefault() {
        return includeReposDefault;
    }
}
