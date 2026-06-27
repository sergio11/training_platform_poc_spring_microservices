#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE training_enrollment;
    CREATE DATABASE training_certification;
    CREATE DATABASE training_notification;
    CREATE DATABASE training_catalog;
    CREATE DATABASE training_employee;
EOSQL
