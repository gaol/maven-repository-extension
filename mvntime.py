#!/bin/python

import sys
import os
import os.path

import optparse

import re
from urlparse import urlparse
import urllib2
import base64

MVN_DOWNLOAD_RE = re.compile(r'\[INFO\] Downloaded: ([^\n]*) \(([^\n]*) ([K]?B) at ([^\n]*) KB\/sec\)')

def calculate(url, username = None, password = None):
  result = {}
  f = None
  try:
    f = open(url)
  except IOError:
    if not url.endswith("consoleText"):
      url = url + "/consoleText"
    req = urllib2.Request(url)
    if username is not None:
      base64string = base64.encodestring('%s:%s' % (username, password))[:-1]  
      authheader =  "Basic %s" % base64string 
      req.add_header("Authorization", authheader)
    f = urllib2.urlopen(req)
  if f is None:
    print "Can't open %s" % url
    exit
  for line in f:
    m = MVN_DOWNLOAD_RE.match(line)
    if m is not None:
      host = urlparse(m.group(1)).hostname
      if m.group(3) == 'KB':
        size = float(m.group(2))
      else:
        size = float(m.group(2)) / 1024
      speed = float(m.group(4))
      if host not in result:
        result[host] = {"normal": {"totalSize": 0, "count": 0, "totalTime": 0, "avgSpeed": 0}, "zerospeed": []}
      hostResult = result[host]
      if speed == 0:
        hostResult["zerospeed"].append("%s, size: %.3f" % (m.group(1)[m.group(1).rfind('/') + 1:], size))
        continue
      hostResult["normal"]["totalSize"] += size
      hostResult["normal"]["count"] += 1
      hostResult["normal"]["totalTime"] += float(size / speed)
      hostResult["normal"]["avgSpeed"] = (hostResult["normal"]["totalSize"] / hostResult["normal"]["totalTime"])
  f.close()
  return result
# end of caculateFromStream

def main():
  """
   Caculates Maven Artifacts downloading time from the log.
  """
  usage="%prog [options] LOG-FILE or Jenkins-Job-Link"
  description="""  calculates downloading time from a maven build log file """
  parser = optparse.OptionParser(usage=usage, description = description)
  parser.add_option('-u', '--username', dest='username', type='string', help='User name to access jenkins log in case of Jenkins link')
  parser.add_option('-p', '--password', dest='password', type='string', help='Password to access jenkins log in case of Jenkins link')
  options, args = parser.parse_args()
  if len(args) != 1:
    parser.print_help()
    exit()
  result = calculate(args[0], username = options.username, password = options.password)
  if len(result.keys()) == 0:
    print "No Maven Artifacts Downloaded found!"
    exit
  print "\nRepositories are: %s" % ", ".join(result.keys())
  for k in result.keys():
    print "\nDownloaded artifacts from host '%s' :" % k
    print "\tTotal Size: %.3f KB, \tTotal Number: %d, \tAverage Speed: %.3f KB/sec" % (result[k]["normal"]["totalSize"], result[k]["normal"]["count"], result[k]["normal"]["avgSpeed"])
    if len(result[k]["zerospeed"]) > 0:
      print "\nThere are %d artifacts downloaded with 0 speed: (Not counted in above total number)" % len(result[k]["zerospeed"])
      print "\n\t" + "\n\t".join(result[k]["zerospeed"])

if __name__ == '__main__':
  main()
