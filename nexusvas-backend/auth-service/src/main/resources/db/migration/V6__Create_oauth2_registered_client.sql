-- Auth Service: OAuth2 Registered Clients (Spring Authorization Server)

CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP WITH TIME ZONE,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(500) NOT NULL,
    authorization_grant_types VARCHAR(500) NOT NULL,
    redirect_uris VARCHAR(500),
    post_logout_redirect_uris VARCHAR(500),
    scopes VARCHAR(500) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

CREATE INDEX idx_oauth2_client_id ON oauth2_registered_client(client_id);
