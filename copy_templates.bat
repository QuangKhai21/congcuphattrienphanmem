@echo off
set SRC=d:\j2ee\J2EE_QuanLyThuCung\src\main\resources\templates
set DST=d:\j2ee\J2EE_QuanLyThuCung\build\resources\main\templates

copy "%SRC%\pets\list.html" "%DST%\pets\list.html"
copy "%SRC%\services\list.html" "%DST%\services\list.html"
copy "%SRC%\vet-qa\my-questions.html" "%DST%\vet-qa\my-questions.html"
copy "%SRC%\lost-pets\my-reports.html" "%DST%\lost-pets\my-reports.html"
copy "%SRC%\health\dashboard.html" "%DST%\health\dashboard.html"
copy "%SRC%\fragments\nav.html" "%DST%\fragments\nav.html"
echo Done
