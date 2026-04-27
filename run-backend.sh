#!/bin/bash
export $(grep -v '^#' config.env | xargs)
cd backend
mvn spring-boot:run
