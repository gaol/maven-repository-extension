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

import java.util.List;

import org.jboss.maven.extensions.repository.OrderedRepositoryConfiguration.OrderRule;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
public class OrderedRepositoryConfigurationTest {

    @Test
    public void testGAVRegex() {
        String redhatArtifacts = "[^\\n]*:[^\\n]*:[^\\n]*redhat-[^\\n]*";
        String mavenCentralArtifacts = "org.apache.maven.plugins:[^\\n]*:[^\\n]*";
        String jbosspublicArtifacts = "org.[jboss|wildfly][^\\n]*:[^\\n]*:[^\\n]*";

        String gav = "org.apache.maven.plugins:maven-resources-plugin:2.7.redhat-1";
        Assert.assertTrue(gav.matches(redhatArtifacts));
        Assert.assertTrue(gav.matches(mavenCentralArtifacts));
        Assert.assertFalse(gav.matches(jbosspublicArtifacts));

        gav = "org.apache.maven.plugins:maven-resources-plugin:2.6";
        Assert.assertFalse(gav.matches(redhatArtifacts));
        Assert.assertTrue(gav.matches(mavenCentralArtifacts));
        Assert.assertFalse(gav.matches(jbosspublicArtifacts));

        gav = "org.jboss.ironjacamar:ironjacamar-core-api:1.0.35.Final";
        Assert.assertFalse(gav.matches(redhatArtifacts));
        Assert.assertFalse(gav.matches(mavenCentralArtifacts));
        Assert.assertTrue(gav.matches(jbosspublicArtifacts));

        gav = "org.wildfly:wildfly-connector:9.0.2.Final";
        Assert.assertFalse(gav.matches(redhatArtifacts));
        Assert.assertFalse(gav.matches(mavenCentralArtifacts));
        Assert.assertTrue(gav.matches(jbosspublicArtifacts));
    }

    @Test
    public void testReadConfigureFiles() throws Exception {
        String additionalConfig = "test-config.properties";
        OrderedRepositoryConfiguration config = new OrderedRepositoryConfiguration(additionalConfig);
        String[] defaultRepos = config.getDefaultRepos();
        Assert.assertEquals(2, defaultRepos.length);
        Assert.assertEquals("jboss-public-repository-group", defaultRepos[0]);
        Assert.assertEquals("central", defaultRepos[1]);

        List<OrderRule> rules = config.getOrderedRules();
        Assert.assertEquals(3, rules.size());

        // Red Hat product artifacts
        OrderRule c1 = rules.get(0);
        Assert.assertEquals(1, c1.getIndex());
        Assert.assertEquals("[^\\n]*:[^\\n]*:[^\\n]*redhat-[^\\n]*", c1.getPattern().pattern());
        String[] c1Repos = c1.getRepos();
        Assert.assertEquals("jboss-eap-7.1-product-repository", c1Repos[0].trim());
        Assert.assertEquals("jboss-product-repository", c1Repos[1].trim());
        Assert.assertEquals("jboss-public-repository-group", c1Repos[2].trim());

        // Maven central artifacts
        OrderRule c2 = rules.get(1);
        Assert.assertEquals(2, c2.getIndex());
        Assert.assertEquals("org.apache.maven.plugins:[^\\n]*:[^\\n]*", c2.getPattern().pattern());
        String[] c2Repos = c2.getRepos();
        Assert.assertEquals("central", c2Repos[0].trim());
        Assert.assertEquals("jboss-public-repository-group", c2Repos[1].trim());
        Assert.assertEquals("jboss-eap-7.1-product-repository", c2Repos[2].trim());
        Assert.assertEquals("jboss-product-repository", c2Repos[3].trim());

        // JBoss community artifacts
        OrderRule c3 = rules.get(2);
        Assert.assertEquals(3, c3.getIndex());
        Assert.assertEquals("org.[jboss|wildfly][^\\n]*:[^\\n]*:[^\\n]*", c3.getPattern().pattern());
        String[] c3Repos = c3.getRepos();
        Assert.assertEquals("jboss-public-repository-group", c3Repos[0].trim());
        Assert.assertEquals("central", c3Repos[1].trim());
        Assert.assertEquals("jboss-eap-7.1-product-repository", c3Repos[2].trim());
        Assert.assertEquals("jboss-product-repository", c3Repos[3].trim());
        
    }
}