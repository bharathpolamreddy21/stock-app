@echo off
echo Stopping PostgreSQL container...
docker stop stockroom-db
docker rm stockroom-db
echo Done.
