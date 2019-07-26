set ip=101.91.119.66
set domain=m2motive.com
(
echo %ip% jenkins.%domain%
echo %ip% sonarqube.%domain%
echo %ip% file.%domain%
echo %ip% gitlab.%domain%
)>>%windir%/System32/drivers/etc/hosts
pause