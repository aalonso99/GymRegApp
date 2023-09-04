from pydrive.drive import GoogleDrive
from pydrive.auth import GoogleAuth
   
# For using listdir()
import os
   
 
def upload_and_remove(path): 
	
	# Below code does the authentication
	# part of the code
	gauth = GoogleAuth()
	  
	# Creates local webserver and auto
	# handles authentication.
	os.chdir('/storage/emulated/0/Android/data/com.iotdatcom.gymregapp/files')
	
	print(open('client_secrets.json',mode='r').read())
 
	gauth.LocalWebserverAuth()       
	drive = GoogleDrive(gauth)
	os.chdir('/')
	   
	# iterating thought all the files/folder
	# of the desired directory
	for x in os.listdir(path):
	   
		f = drive.CreateFile({'title': x})
		f.SetContentFile(os.path.join(path, x))
		f.Upload()
	  
		# Due to a known bug in pydrive if we 
		# don't empty the variable used to
		# upload the files to Google Drive the
		# file stays open in memory and causes a
		# memory leak, therefore preventing its 
		# deletion
		f = None
		
		# We remove the file
		#os.remove(os.path.join(path, x))
