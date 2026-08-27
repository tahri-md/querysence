-- Create databases with proper encoding
CREATE DATABASE IF NOT EXISTS querysence
    WITH ENCODING = 'UTF8' TEMPLATE = template0 LC_COLLATE = 'C' LC_CTYPE = 'C';

CREATE DATABASE IF NOT EXISTS keycloak
    WITH ENCODING = 'UTF8' TEMPLATE = template0 LC_COLLATE = 'C' LC_CTYPE = 'C';

-- Grant permissions to amine user
GRANT ALL PRIVILEGES ON DATABASE querysence TO amine;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO amine;
