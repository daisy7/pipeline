timeLog(){
	for i in "$@";do 
        $i|awk '{print "###" strftime("%y/%m/%d/%H:%M:%S",systime()) "\t" $0 "\n"}'
    done
}
chown 100:users -R *
timeLog "svn add Datasheet/*"
timeLog "svn commit -m 'daily submission' Datasheet"
timeLog "svn checkout https://101.91.118.224:5078/svn/硬件器件库/EDA/LIB/ EDA/LIB/"
cd EDA/LIB/ && timeLog "unzip -oq Orcad_LIB.zip"
chown 100:users -R *
timeLog "echo zip file unzip success"
str=$(printf "%-100s" "*")
echo "${str// /*}"