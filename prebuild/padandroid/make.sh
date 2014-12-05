 #!/bin/sh  

productName=""

function getProductName(){
	productName=${DEVICE##*/}
	echo "lunch proudct name: "$productName
}
function check()
{	
	if [ ! -d "./out/target/product/$productName/system/app/" ]; then
		echo "Err:product system folder don't exists,please ensure you have build the product";
		return 0;
	fi
	if [ ! -d "./out/target/product/$productName/system/etc/" ]; then
		echo "Err:product system folder don't exists,please ensure you have build the product";
		return 0;
	fi
	copyapks "com.padandroid.launcher.fit.apk" "com.padandroid.theme.Sky.apk" "com.padandroid.aplus.apk" "com.padandroid.theme.tea.apk"
	copyfiles "default_workspace.xml"
}

function copyapks()
{
	for var in $*; do
		if [ ! -f "./padandroid/$var" ]; then  
			echo "$var files not exists, copy failed";
		else
			cp "./padandroid/$var" "./out/target/product/$productName/system/app/"
			echo "$var copy succeed!";
		fi 
	done
	
	
}
function copyfiles()
{
	for var in $*; do
		if [ ! -f "./padandroid/$var" ]; then  
			echo "$var files not exists, copy failed";
		else
			cp "./padandroid/$var" "./out/target/product/$productName/system/etc/"
			echo "$var copy succeed!";
		fi 
	done
	
	
}
getProductName;
check;
echo "over"


