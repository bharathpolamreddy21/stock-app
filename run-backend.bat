@echo off
setlocal enabledelayedexpansion
REM Load environment variables from config.env (skip comments and blank lines)
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("config.env") do (
    if not "%%A"=="" if not "%%B"=="" (
        set "%%A=%%B"
    )
)
cd backend
mvn spring-boot:run
