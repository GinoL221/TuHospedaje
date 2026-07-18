CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    description TEXT,
    icon VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_categories_name UNIQUE (name)
);

CREATE TABLE features (
    id BIGINT NOT NULL AUTO_INCREMENT,
    icon VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_features_name UNIQUE (name)
);

CREATE TABLE lodgings (
    max_guests INT,
    price_per_night DECIMAL(10,2),
    category_id BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    description TEXT,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_lodgings_email UNIQUE (email),
    CONSTRAINT FK_lodgings_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    description TEXT,
    icon VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(255),
    last_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM ('ADMIN', 'USER') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_users_email UNIQUE (email)
);

CREATE TABLE lodging_features (
    feature_id BIGINT NOT NULL,
    lodging_id BIGINT NOT NULL,
    PRIMARY KEY (feature_id, lodging_id),
    INDEX IX_lodging_features_lodging (lodging_id),
    CONSTRAINT FK_lodging_features_feature FOREIGN KEY (feature_id) REFERENCES features (id),
    CONSTRAINT FK_lodging_features_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id)
);

CREATE TABLE lodging_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lodging_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    PRIMARY KEY (id),
    INDEX IX_lodging_images_lodging (lodging_id),
    CONSTRAINT FK_lodging_images_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id)
);

CREATE TABLE lodging_policies (
    lodging_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    PRIMARY KEY (lodging_id, policy_id),
    INDEX IX_lodging_policies_policy (policy_id),
    CONSTRAINT FK_lodging_policies_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id),
    CONSTRAINT FK_lodging_policies_policy FOREIGN KEY (policy_id) REFERENCES policies (id)
);

CREATE TABLE ratings (
    score INT NOT NULL CHECK (score <= 5 AND score >= 1),
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    lodging_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment TEXT,
    PRIMARY KEY (id),
    INDEX IX_ratings_lodging (lodging_id),
    INDEX IX_ratings_user (user_id),
    CONSTRAINT FK_ratings_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id),
    CONSTRAINT FK_ratings_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE reservations (
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    lodging_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    version BIGINT,
    guest_email VARCHAR(255) NOT NULL,
    guest_name VARCHAR(255) NOT NULL,
    guest_phone VARCHAR(255) NOT NULL,
    status ENUM ('CANCELLED', 'CONFIRMED') NOT NULL,
    PRIMARY KEY (id),
    INDEX IX_reservations_lodging (lodging_id),
    INDEX IX_reservations_user (user_id),
    CONSTRAINT FK_reservations_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id),
    CONSTRAINT FK_reservations_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_favorites (
    lodging_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (lodging_id, user_id),
    INDEX IX_user_favorites_user (user_id),
    CONSTRAINT FK_user_favorites_lodging FOREIGN KEY (lodging_id) REFERENCES lodgings (id),
    CONSTRAINT FK_user_favorites_user FOREIGN KEY (user_id) REFERENCES users (id)
);
