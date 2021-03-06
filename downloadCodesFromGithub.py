import subprocess
import shlex
import csv
import timeit
import time
import os, sys

'''
	Author: Chengjun Yuan <cy3yb@virginia.edu>
	Time:	Jan.29.2016
	how to run: python downloadCodesFromGithub.py projectsList.csv downloadProjectsPath
'''

# read URLs from javaProjectsURLs.csv 
def readUrls(fileName):
	csvFile = open(fileName)
	csvRows = csv.reader(csvFile, delimiter=',')
	URLs = []
	i = 0
	for row in csvRows:
		if i == 0:		# The first row is column 
			i += 1
			continue
		index = row[1].find('repos') + 6
		URL = row[1][index:]
		URLs.append(URL)
		if i < 20:
			print URL
		i = i + 1
	
	csvFile.close
	return URLs

if __name__ == "__main__":
	if len(sys.argv) != 3:
		print "Usage: python downloadCodesFromGithub.py projectsList.csv downloadProjectsPath"
		sys.exit()
	URLs = readUrls(sys.argv[1])
	print 'URL',len(URLs)
	os.chdir(sys.argv[2])
	retval = os.getcwd()
	print "Directory changed to %s" % retval
	i=0
	for oneURL in URLs:
		i = i + 1
		if i > 1000:  # the number of projects to download
			break
		index = oneURL.find('/')
		folder = oneURL[index + 1 :]
		if os.path.isdir(folder) == True : # if the project has been downloaded, then pass to the next project
			continue
		command = 'git clone git@github.com:'+oneURL+'.git'
		print i, command
		args = shlex.split(command)
		startTime = timeit.default_timer()
		p = subprocess.Popen(args)
		p.wait()
		usedTime = timeit.default_timer() - startTime
		if usedTime < 3 :
			time.sleep(3 - usedTime)
		
	

	
