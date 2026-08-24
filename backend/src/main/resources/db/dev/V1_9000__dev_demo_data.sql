-- ============================================================
-- Development-only, explicit opt-in demo data for TuHospedaje
-- Requires Flyway placeholder dev_admin_password_hash from DEV_ADMIN_PASSWORD_HASH.
-- The application first requires "dev" as the sole active profile. This database-name
-- check is defense-in-depth. The preflight performs no persistent writes and catches
-- known invalid inputs before the first strict INSERT. MariaDB does not make the whole
-- migration transactional: after any failure, drop and recreate the disposable dev
-- database before retrying.

CREATE TEMPORARY TABLE dev_seed_guard (
    database_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    seed_tables_empty BOOLEAN NOT NULL,
    CONSTRAINT chk_dev_seed_database_name CHECK (
        LOWER(database_name) REGEXP '(^|_)(dev|test)(_|$)'
        AND LOWER(database_name) NOT REGEXP '(^|_)(prod|production|stage|staging|uat)(_|$)'
    ),
    CONSTRAINT chk_dev_seed_tables_empty CHECK (seed_tables_empty = TRUE),
    CONSTRAINT chk_dev_seed_bcrypt_hash CHECK (
        CHAR_LENGTH(password_hash) = 60
        AND LEFT(password_hash, 4) IN ('$2a$', '$2b$', '$2y$')
        AND SUBSTRING(password_hash, 5, 2) REGEXP '^[0-9]{2}$'
        AND CAST(SUBSTRING(password_hash, 5, 2) AS UNSIGNED) BETWEEN 10 AND 14
        AND SUBSTRING(password_hash, 7, 1) = '$'
        AND SUBSTRING(password_hash, 8) REGEXP '^[./A-Za-z0-9]{53}$'
    )
);
INSERT INTO dev_seed_guard (database_name, password_hash, seed_tables_empty)
SELECT DATABASE(), '${dev_admin_password_hash}',
       (SELECT COUNT(*) FROM categories) = 0
       AND (SELECT COUNT(*) FROM features) = 0
       AND (SELECT COUNT(*) FROM policies) = 0
       AND (SELECT COUNT(*) FROM users) = 0
       AND (SELECT COUNT(*) FROM lodgings) = 0
       AND (SELECT COUNT(*) FROM lodging_images) = 0
       AND (SELECT COUNT(*) FROM lodging_features) = 0
       AND (SELECT COUNT(*) FROM lodging_policies) = 0;
DROP TEMPORARY TABLE dev_seed_guard;
-- ============================================================

-- Categories
INSERT INTO categories (id, name, description, icon) VALUES
(1, 'Hoteles', 'Hoteles urbanos y de negocios', 'hotel'),
(2, 'Cabañas', 'Cabañas rústicas en la naturaleza', 'tree-pine'),
(3, 'Departamentos', 'Departamentos céntricos totalmente equipados', 'building-2'),
(4, 'Hostels', 'Hostels económicos y sociales', 'bed-double'),
(5, 'Resorts', 'Resorts y complejos de lujo', 'water'),
(6, 'Glamping', 'Glamping y naturaleza con comodidades', 'tent');

-- Features
INSERT INTO features (id, name, icon) VALUES
(1, 'WiFi gratis', 'wifi'),
(2, 'Estacionamiento', 'car'),
(3, 'Aire acondicionado', 'thermometer-snowflake'),
(4, 'Desayuno incluido', 'utensils'),
(5, 'Pileta', 'water'),
(6, 'Mascotas permitidas', 'paw-print'),
(7, 'TV', 'tv'),
(8, 'Cocina equipada', 'kitchen-set');

-- Policies
INSERT INTO policies (id, name, description, icon) VALUES
(1, 'Check-in', 'A partir de las 14:00', 'clock'),
(2, 'Check-out', 'Hasta las 11:00', 'clock'),
(3, 'Cancelación', 'Cancelación gratuita hasta 48 horas antes del check-in', 'ban'),
(4, 'Fumadores', 'No se permite fumar en las habitaciones', 'smoking-ban'),
(5, 'Mascotas', 'Mascotas pequeñas permitidas con cargo adicional', 'paw-print'),
(6, 'Fiestas', 'No se permiten fiestas ni eventos', 'party-popper');

-- Administrator password is supplied at migration time; no credential is stored in source.
INSERT INTO users (id, first_name, last_name, email, password, role) VALUES
(1, 'Admin', 'TuHospedaje', 'admin@tuhospedaje.com', '${dev_admin_password_hash}', 'ADMIN');

-- Lodgings
INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
(1, 'Hotel Buenos Aires Centro', 'Hotel céntrico con vista al obelisco. Habitaciones amplias y modernas con baño privado, TV LED y aire acondicionado. Cuenta con restaurante, bar y sala de negocios.', 'Av. Corrientes 1234', 'Buenos Aires', 'Argentina', '+54111234567', 'centro@hotelba.com', 1, 150.00, 4),
(2, 'Cabaña Los Arrayanes', 'Hermosa cabaña de montaña con vista al lago. Rodeada de bosques nativos, ideal para desconectarse. Incluye chimenea, hidromasaje y fogón exterior.', 'Ruta 40 Km 2050', 'Bariloche', 'Argentina', '+54294456789', 'arrayanes@cabanas.com', 2, 200.00, 6),
(3, 'Departamento Palermo Soho', 'Moderno departamento en el barrio más trendy de Buenos Aires. Cerca de bares, restaurantes y tiendas de diseño. Cuenta con balcón, cocina completa y laundry.', 'Gurruchaga 2100', 'Buenos Aires', 'Argentina', '+541198765432', 'palermo@departamento.com', 3, 180.00, 4),
(4, 'Hostel Córdoba Backpackers', 'Hostel para mochileros con ambiente internacional. Incluye desayuno, WiFi, y actividades grupales. Habitaciones compartidas y privadas.', 'Av. Recta Martinoli 5432', 'Córdoba', 'Argentina', '+54351123456', 'backpackers@hostelcba.com', 4, 50.00, 8),
(5, 'Cabaña del Lago', 'Cabaña frente al lago Nahuel Huapi con muelle privado. Ideal para parejas. Incluye kayaks y bicicletas de cortesía.', 'Av. Bustillo 4500', 'Bariloche', 'Argentina', '+54294432123', 'lago@cabanas.com', 2, 250.00, 4),
(6, 'Hotel Internacional', 'Hotel 4 estrellas en pleno centro de Mendoza. Cerca de bodegas y plazas. Pileta climatizada, spa y restaurante gourmet.', 'Av. San Martín 850', 'Mendoza', 'Argentina', '+54261456789', 'internacional@hotelmza.com', 1, 220.00, 4),
(7, 'Cabaña El Mirador', 'Cabaña con vista panorámica al valle de Salta. Rodeada de cerros, ideal para desconectarse. Incluye chimenea, hamaca paraguaya y fogón exterior con vista.', 'Ruta 51 Km 15', 'Salta', 'Argentina', '+54387456123', 'mirador@cabanas.com', 2, 180.00, 5),
(8, 'Hotel Mar del Plata', 'Hotel frente al mar con vista panorámica al océano. Habitaciones amplias con balcón privado. Cuenta con pileta climatizada, spa y restaurante de mariscos.', 'Av. Peralta Ramos 3500', 'Mar del Plata', 'Argentina', '+54223567890', 'mdp@hotelmardelplata.com', 1, 280.00, 4),
(9, 'Departamento Puerto Madero', 'Exclusivo departamento en Puerto Madero con vista al dique. Cerca de los mejores restaurantes de la ciudad. Cuenta con cocina equipada, laundry y balcón.', 'Juana Manso 1200', 'Buenos Aires', 'Argentina', '+541167890123', 'puertomadero@departamento.com', 3, 320.00, 6),
(10, 'Hostel Salta Andino', 'Hostel temático andino en el centro de Salta. Ambiente internacional con decoración regional. Incluye desayuno, WiFi y excursiones grupales.', 'Balcarce 850', 'Salta', 'Argentina', '+54387456789', 'salta@hostelandino.com', 4, 45.00, 10);

-- Images (5 per lodging)
INSERT INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Hotel Buenos Aires Centro
(1, 'http://localhost:8080/canonical-lodging-images/lodging-001-hotel-buenos-aires-centro/masters/lodging-001-scene-01-fachada-acceso-urbano-v001.jpg', 'Hotel Buenos Aires Centro - Fachada', 1),
(2, 'http://localhost:8080/canonical-lodging-images/lodging-001-hotel-buenos-aires-centro/masters/lodging-001-scene-02-habitacion-doble-v001.jpg', 'Hotel Buenos Aires Centro - Habitación', 1),
(3, 'http://localhost:8080/canonical-lodging-images/lodging-001-hotel-buenos-aires-centro/masters/lodging-001-scene-03-bano-privado-v001.jpg', 'Hotel Buenos Aires Centro - Baño', 1),
(4, 'http://localhost:8080/canonical-lodging-images/lodging-001-hotel-buenos-aires-centro/masters/lodging-001-scene-04-restaurante-bar-v001.jpg', 'Hotel Buenos Aires Centro - Pileta', 1),
(5, 'http://localhost:8080/canonical-lodging-images/lodging-001-hotel-buenos-aires-centro/masters/lodging-001-scene-05-lobby-trabajo-reuniones-v001.jpg', 'Hotel Buenos Aires Centro - Suite', 1),
-- Cabaña Los Arrayanes
(6, 'http://localhost:8080/canonical-lodging-images/lodging-002-cabana-los-arrayanes/masters/lodging-002-scene-01-exterior-entre-bosque-v001.jpg', 'Cabaña Los Arrayanes - Exterior', 2),
(7, 'http://localhost:8080/canonical-lodging-images/lodging-002-cabana-los-arrayanes/masters/lodging-002-scene-02-dormitorio-principal-v001.jpg', 'Cabaña Los Arrayanes - Interior con chimenea', 2),
(8, 'http://localhost:8080/canonical-lodging-images/lodging-002-cabana-los-arrayanes/masters/lodging-002-scene-03-bano-piedra-jacuzzi-v001.jpg', 'Cabaña Los Arrayanes - Vista al lago', 2),
(9, 'http://localhost:8080/canonical-lodging-images/lodging-002-cabana-los-arrayanes/masters/lodging-002-scene-04-estar-cocina-comedor-v001.jpg', 'Cabaña Los Arrayanes - Habitación', 2),
(10, 'http://localhost:8080/canonical-lodging-images/lodging-002-cabana-los-arrayanes/masters/lodging-002-scene-05-fogon-bosque-v001.jpg', 'Cabaña Los Arrayanes - Baño', 2),
-- Departamento Palermo Soho
(11, 'http://localhost:8080/canonical-lodging-images/lodging-003-departamento-palermo-soho/masters/lodging-003-scene-01-fachada-urbana-v001.jpg', 'Departamento Palermo - Living', 3),
(12, 'http://localhost:8080/canonical-lodging-images/lodging-003-departamento-palermo-soho/masters/lodging-003-scene-02-dormitorio-principal-v001.jpg', 'Departamento Palermo - Dormitorio', 3),
(13, 'http://localhost:8080/canonical-lodging-images/lodging-003-departamento-palermo-soho/masters/lodging-003-scene-03-bano-privado-v001.jpg', 'Departamento Palermo - Baño', 3),
(14, 'http://localhost:8080/canonical-lodging-images/lodging-003-departamento-palermo-soho/masters/lodging-003-scene-04-cocina-comedor-integrados-v001.jpg', 'Departamento Palermo - Cocina', 3),
(15, 'http://localhost:8080/canonical-lodging-images/lodging-003-departamento-palermo-soho/masters/lodging-003-scene-05-balcon-equipado-v001.jpg', 'Departamento Palermo - Living completo', 3),
-- Hostel Córdoba Backpackers
(16, 'http://localhost:8080/canonical-lodging-images/lodging-004-hostel-cordoba-backpackers/masters/lodging-004-scene-01-fachada-casa-reciclada-v001.jpg', 'Hostel Córdoba - Sala común', 4),
(17, 'http://localhost:8080/canonical-lodging-images/lodging-004-hostel-cordoba-backpackers/masters/lodging-004-scene-02-dormitorio-compartido-v001.jpg', 'Hostel Córdoba - Dormitorio compartido', 4),
(18, 'http://localhost:8080/canonical-lodging-images/lodging-004-hostel-cordoba-backpackers/masters/lodging-004-scene-03-bano-compartido-v001.jpg', 'Hostel Córdoba - Baño', 4),
(19, 'http://localhost:8080/canonical-lodging-images/lodging-004-hostel-cordoba-backpackers/masters/lodging-004-scene-04-cocina-comedor-v001.jpg', 'Hostel Córdoba - Cocina', 4),
(20, 'http://localhost:8080/canonical-lodging-images/lodging-004-hostel-cordoba-backpackers/masters/lodging-004-scene-05-patio-galeria-v001.jpg', 'Hostel Córdoba - Fachada', 4),
-- Cabaña del Lago
(21, 'http://localhost:8080/canonical-lodging-images/lodging-005-cabana-del-lago/masters/lodging-005-scene-01-exterior-costa-v001.jpg', 'Cabaña del Lago - Living con fogón', 5),
(22, 'http://localhost:8080/canonical-lodging-images/lodging-005-cabana-del-lago/masters/lodging-005-scene-02-dormitorio-principal-v001.jpg', 'Cabaña del Lago - Interior acogedor', 5),
(23, 'http://localhost:8080/canonical-lodging-images/lodging-005-cabana-del-lago/masters/lodging-005-scene-03-bano-privado-v001.jpg', 'Cabaña del Lago - Habitación', 5),
(24, 'http://localhost:8080/canonical-lodging-images/lodging-005-cabana-del-lago/masters/lodging-005-scene-04-estar-cocina-hogar-v001.jpg', 'Cabaña del Lago - Sala de estar', 5),
(25, 'http://localhost:8080/canonical-lodging-images/lodging-005-cabana-del-lago/masters/lodging-005-scene-05-muelle-kayaks-v001.jpg', 'Cabaña del Lago - Baño', 5),
-- Hotel Internacional
(26, 'http://localhost:8080/canonical-lodging-images/lodging-006-hotel-internacional/masters/lodging-006-scene-01-fachada-acceso-cubierto-v001.jpg', 'Hotel Internacional - Habitación', 6),
(27, 'http://localhost:8080/canonical-lodging-images/lodging-006-hotel-internacional/masters/lodging-006-scene-02-habitacion-superior-v001.jpg', 'Hotel Internacional - Pileta', 6),
(28, 'http://localhost:8080/canonical-lodging-images/lodging-006-hotel-internacional/masters/lodging-006-scene-03-bano-privado-v001.jpg', 'Hotel Internacional - Suite', 6),
(29, 'http://localhost:8080/canonical-lodging-images/lodging-006-hotel-internacional/masters/lodging-006-scene-04-restaurante-gastronomico-v001.jpg', 'Hotel Internacional - Baño', 6),
(30, 'http://localhost:8080/canonical-lodging-images/lodging-006-hotel-internacional/masters/lodging-006-scene-05-piscina-spa-v001.jpg', 'Hotel Internacional - Fachada', 6),
-- Cabaña El Mirador
(31, 'http://localhost:8080/canonical-lodging-images/lodging-007-cabana-el-mirador/masters/lodging-007-scene-01-exterior-ladera-v001.jpg', 'Cabaña El Mirador - Interior con vista', 7),
(32, 'http://localhost:8080/canonical-lodging-images/lodging-007-cabana-el-mirador/masters/lodging-007-scene-02-dormitorio-vista-valle-v001.jpg', 'Cabaña El Mirador - Cartel del valle', 7),
(33, 'http://localhost:8080/canonical-lodging-images/lodging-007-cabana-el-mirador/masters/lodging-007-scene-03-bano-microcemento-v001.jpg', 'Cabaña El Mirador - Fogón exterior', 7),
(34, 'http://localhost:8080/canonical-lodging-images/lodging-007-cabana-el-mirador/masters/lodging-007-scene-04-estar-cocina-integrados-v001.jpg', 'Cabaña El Mirador - Entorno natural', 7),
(35, 'http://localhost:8080/canonical-lodging-images/lodging-007-cabana-el-mirador/masters/lodging-007-scene-05-fogon-atardecer-v001.jpg', 'Cabaña El Mirador - Baño', 7),
-- Hotel Mar del Plata
(36, 'http://localhost:8080/canonical-lodging-images/lodging-008-hotel-mar-del-plata/masters/lodging-008-scene-01-fachada-costera-v001.jpg', 'Hotel Mar del Plata - Fachada', 8),
(37, 'http://localhost:8080/canonical-lodging-images/lodging-008-hotel-mar-del-plata/masters/lodging-008-scene-02-habitacion-vista-mar-v001.jpg', 'Hotel Mar del Plata - Habitación', 8),
(38, 'http://localhost:8080/canonical-lodging-images/lodging-008-hotel-mar-del-plata/masters/lodging-008-scene-03-bano-privado-v001.jpg', 'Hotel Mar del Plata - Pileta', 8),
(39, 'http://localhost:8080/canonical-lodging-images/lodging-008-hotel-mar-del-plata/masters/lodging-008-scene-04-restaurante-oceano-v001.jpg', 'Hotel Mar del Plata - Restaurante', 8),
(40, 'http://localhost:8080/canonical-lodging-images/lodging-008-hotel-mar-del-plata/masters/lodging-008-scene-05-piscina-spa-v001.jpg', 'Hotel Mar del Plata - Habitación vista', 8),
-- Departamento Puerto Madero
(41, 'http://localhost:8080/canonical-lodging-images/lodging-009-departamento-puerto-madero/masters/lodging-009-scene-01-torre-acceso-agua-v001.jpg', 'Puerto Madero - Vista al dique', 9),
(42, 'http://localhost:8080/canonical-lodging-images/lodging-009-departamento-puerto-madero/masters/lodging-009-scene-02-dormitorio-principal-v001.jpg', 'Puerto Madero - Living', 9),
(43, 'http://localhost:8080/canonical-lodging-images/lodging-009-departamento-puerto-madero/masters/lodging-009-scene-03-bano-privado-v001.jpg', 'Puerto Madero - Dormitorio', 9),
(44, 'http://localhost:8080/canonical-lodging-images/lodging-009-departamento-puerto-madero/masters/lodging-009-scene-04-living-comedor-cocina-v001.jpg', 'Puerto Madero - Sala comedor', 9),
(45, 'http://localhost:8080/canonical-lodging-images/lodging-009-departamento-puerto-madero/masters/lodging-009-scene-05-piscina-climatizada-v001.jpg', 'Puerto Madero - Vestidor', 9),
-- Hostel Salta Andino
(46, 'http://localhost:8080/canonical-lodging-images/lodging-010-hostel-salta-andino/masters/lodging-010-scene-01-fachada-restaurada-v001.jpg', 'Hostel Salta - Habitación', 10),
(47, 'http://localhost:8080/canonical-lodging-images/lodging-010-hostel-salta-andino/masters/lodging-010-scene-02-dormitorio-compartido-v001.jpg', 'Hostel Salta - Sala de estar', 10),
(48, 'http://localhost:8080/canonical-lodging-images/lodging-010-hostel-salta-andino/masters/lodging-010-scene-03-bano-compartido-v001.jpg', 'Hostel Salta - Habitación privada', 10),
(49, 'http://localhost:8080/canonical-lodging-images/lodging-010-hostel-salta-andino/masters/lodging-010-scene-04-cocina-comedor-v001.jpg', 'Hostel Salta - Patio', 10),
(50, 'http://localhost:8080/canonical-lodging-images/lodging-010-hostel-salta-andino/masters/lodging-010-scene-05-patio-galerias-v001.jpg', 'Hostel Salta - Fachada', 10);

-- Lodging-feature relationships
INSERT INTO lodging_features (lodging_id, feature_id) VALUES
(1, 1), (1, 3), (1, 7), (1, 2),
(2, 1), (2, 2), (2, 6), (2, 5),
(3, 1), (3, 3), (3, 7), (3, 8),
(4, 1), (4, 2), (4, 4),
(5, 1), (5, 2), (5, 5), (5, 6),
(6, 1), (6, 3), (6, 5), (6, 7), (6, 2),
(7, 1), (7, 2), (7, 6), (7, 5),
(8, 1), (8, 3), (8, 5), (8, 7), (8, 2),
(9, 1), (9, 3), (9, 7), (9, 8), (9, 2),
(10, 1), (10, 2), (10, 4);

-- Lodging-policy relationships
INSERT INTO lodging_policies (lodging_id, policy_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 2), (2, 3), (2, 5), (2, 6),
(3, 1), (3, 2), (3, 3), (3, 4),
(4, 1), (4, 2), (4, 3), (4, 6),
(5, 1), (5, 2), (5, 3), (5, 5),
(6, 1), (6, 2), (6, 3), (6, 4), (6, 6),
(7, 1), (7, 2), (7, 3), (7, 5),
(8, 1), (8, 2), (8, 3), (8, 4), (8, 6),
(9, 1), (9, 2), (9, 3), (9, 4),
(10, 1), (10, 2), (10, 3), (10, 6);

-- ============================================================
-- Sprint 4 seed extension (lodging IDs 11–30)
-- ============================================================

INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
(11, 'Hotel Riviera Rosario', 'Moderno hotel boutique frente al río Paraná. Diseño contemporáneo con vistas panorámicas, restaurante de cocina de autor y terraza bar. Ideal para viajes de negocios y turismo.', 'Av. Belgrano 1056', 'Rosario', 'Argentina', '+54341555123', 'riviera@hotelrosario.com', 1, 170.00, 4),
(12, 'Cabaña Los Cipreses', 'Cabaña de montaña rodeada de cipreses y arrayanes en Villa La Angostura. Chimenea a leña, hidromasaje exterior y acceso directo al lago Correntoso. Perfecta para parejas y familias.', 'Calle Los Arrayanes 450', 'Villa La Angostura', 'Argentina', '+54294470123', 'cipreses@cabanas.com', 2, 230.00, 6),
(13, 'Departamento Nueva Córdoba', 'Luminoso departamento en el corazón del barrio universitario de Córdoba. Cocina completa, balcón con vistas a la ciudad y a pasos de bares y restaurantes. Ideal para estadías largas.', 'Obispo Trejo 890', 'Córdoba', 'Argentina', '+54351789456', 'nuevacba@departamento.com', 3, 140.00, 3),
(14, 'Gran Hotel Tucumán', 'Hotel de categoría en el centro de San Miguel de Tucumán. Habitaciones amplias con vista a la plaza principal, desayuno buffet incluido, spa y sala de reuniones equipada.', 'San Martín 987', 'Tucumán', 'Argentina', '+54381456789', 'gran@hoteltucuman.com', 1, 160.00, 4),
(15, 'Cabaña El Bolsón', 'Cabaña ecológica en El Bolsón, rodeada de bosques nativos y huertas orgánicas. Incluye desayuno artesanal, fogón y bicicletas. A minutos de la feria regional y senderos de trekking.', 'Ruta 40 Km 1730', 'El Bolsón', 'Argentina', '+54294490456', 'elbolson@cabanas.com', 2, 190.00, 5),
(16, 'Departamento Chacras de Coria', 'Elegante departamento en la zona de bodegas de Mendoza. Amplio salón con diseño moderno, terraza privada y piscina compartida. A 10 minutos de las principales bodegas de la región.', 'Videla Aranda 2340', 'Mendoza', 'Argentina', '+54261789321', 'chacras@departamento.com', 3, 165.00, 4),
(17, 'Hotel del Glaciar', 'Hotel panorámico en Ushuaia con vistas al canal Beagle y las montañas del fin del mundo. Calefacción central, restaurante de especialidades fueguinas y excursiones coordinadas.', 'Av. del Glaciar 255', 'Ushuaia', 'Argentina', '+54290145678', 'glaciar@hotelushaia.com', 1, 310.00, 4),
(18, 'Cabaña La Cumbrecita', 'Cabaña familiar en Villa Carlos Paz, sobre el lago San Roque. Incluye muelle privado, kayaks y bicicletas. Perfecta para desconectarse con vistas al lago y las sierras cordobesas.', 'Av. Costanera 780', 'Villa Carlos Paz', 'Argentina', '+54351567890', 'cumbrecita@cabanas.com', 2, 175.00, 6),
(19, 'Departamento Belgrano', 'Moderno departamento en el barrio de Belgrano, Buenos Aires. Planta alta con balcón corrido, cocina equipada y laundry. A pasos del subte D y el paseo de compras del barrio chino.', 'Juramento 2100', 'Buenos Aires', 'Argentina', '+541145678901', 'belgrano@departamento.com', 3, 200.00, 4),
(20, 'Hotel La Perla', 'Hotel frente a la playa Grande de Mar del Plata. Habitaciones con balcón y vista al mar, pileta climatizada, spa y restaurante de mariscos. A metros de la peatonal y el puerto.', 'Av. Colón 2750', 'Mar del Plata', 'Argentina', '+54223789012', 'laperla@hotelmdp.com', 1, 260.00, 4),
(21, 'Cabaña Lacar', 'Cabaña premium en San Martín de los Andes, frente al lago Lacar. Terraza con vista al lago, canoa incluida y acceso directo a senderos de trekking. Ambiente de montaña patagónico.', 'Av. Koessler 1500', 'San Martín de los Andes', 'Argentina', '+54294456321', 'lacar@cabanas.com', 2, 245.00, 5),
(22, 'Departamento Pichincha', 'Coqueto departamento en el barrio Pichincha de Rosario, a metros del río Paraná. Estilo industrial, cocina completa y terraza privada. Zona gastronómica y cultural de la ciudad.', 'Rodríguez 1340', 'Rosario', 'Argentina', '+54341678901', 'pichincha@departamento.com', 3, 135.00, 3),
(23, 'Hotel Cataratas Iguazú', 'Hotel selva adentro en Puerto Iguazú con acceso privado al parque nacional. Habitaciones con ventanas panorámicas hacia la selva, piscina desbordante y restaurante con cocina regional.', 'Ruta 12 Km 1650', 'Puerto Iguazú', 'Argentina', '+54375789123', 'cataratas@hoteliguazu.com', 1, 280.00, 4),
(24, 'Cabaña Los Glaciares', 'Cabaña de lujo en El Calafate con vista directa al lago Argentino y el glaciar Perito Moreno. Calefacción radiante, jacuzzi con vistas y excursiones al glaciar coordinadas desde el hotel.', 'Av. del Libertador 4500', 'El Calafate', 'Argentina', '+54290269456', 'glaciares@cabanas.com', 2, 295.00, 4),
(25, 'Departamento Recoleta', 'Exclusivo departamento en Recoleta con vista al parque. Techos altos, pisos de madera, cocina de diseño y sala de estar amplia. Edificio histórico refaccionado a metros del cementerio y museos.', 'Av. Callao 1450', 'Buenos Aires', 'Argentina', '+541156789012', 'recoleta@departamento.com', 3, 240.00, 4),
(26, 'Hotel Legado Mítico', 'Hotel boutique en el centro histórico de Salta. Cada habitación evoca un personaje de la historia argentina. Patio colonial, restaurante gourmet y acceso a excursiones a la quebrada de Humahuaca.', 'Mitre 647', 'Salta', 'Argentina', '+54387678901', 'legado@hotelsalta.com', 1, 290.00, 4),
(27, 'Hostel La Plata Centro', 'Hostel céntrico en La Plata, a metros de la diagonal principal y la catedral. Ambiente universitario e internacional, habitaciones privadas y compartidas, cocina común y patio.', 'Diagonal 80 Nro. 456', 'La Plata', 'Argentina', '+54221567890', 'laplata@hostelcentro.com', 4, 40.00, 8),
(28, 'Hostel Patagónico', 'Hostel temático de aventura en Bariloche, ideal para mochileros y deportistas. Incluye desayuno, sala de ski storage, mapas de senderos y conexión con guías de trekking locales.', 'Onelli 550', 'Bariloche', 'Argentina', '+54294478901', 'patagonico@hostelbar.com', 4, 55.00, 10),
(29, 'Hostel San Telmo', 'Hostel en el barrio más bohemio de Buenos Aires, a pasos de la feria de San Telmo y el Caminito. Ambiente artístico, terraza con vista a la ciudad, cocina equipada y tours de tango incluidos.', 'Defensa 890', 'Buenos Aires', 'Argentina', '+541134567890', 'santelmo@hostel.com', 4, 48.00, 10),
(30, 'Hostel Las Viñas', 'Hostel vitivinícola en el corazón de la zona de bodegas de Mendoza. Organiza visitas guiadas a bodegas, catas de vino y excursiones a la montaña. Patio con parrilla y pileta.', 'Emilio Civit 1200', 'Mendoza', 'Argentina', '+54261890123', 'lasvinias@hostel.com', 4, 42.00, 8);

-- Images for lodging IDs 11–30 (5 per lodging, image IDs 51–150)
-- The first image of each lodging is its grid cover.
INSERT INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Hotel Riviera Rosario (11)
(51, 'http://localhost:8080/canonical-lodging-images/lodging-011-hotel-riviera-rosario/masters/lodging-011-scene-01-fachada-rio-atrio-v001.jpg', 'Hotel Riviera - Fachada', 11),
(52, 'http://localhost:8080/canonical-lodging-images/lodging-011-hotel-riviera-rosario/masters/lodging-011-scene-02-habitacion-boutique-v001.jpg', 'Hotel Riviera - Habitación', 11),
(53, 'http://localhost:8080/canonical-lodging-images/lodging-011-hotel-riviera-rosario/masters/lodging-011-scene-03-bano-privado-v001.jpg', 'Hotel Riviera - Piscina', 11),
(54, 'http://localhost:8080/canonical-lodging-images/lodging-011-hotel-riviera-rosario/masters/lodging-011-scene-04-restaurante-autor-v001.jpg', 'Hotel Riviera - Restaurante', 11),
(55, 'http://localhost:8080/canonical-lodging-images/lodging-011-hotel-riviera-rosario/masters/lodging-011-scene-05-terraza-bar-rio-v001.jpg', 'Hotel Riviera - Suite', 11),
-- Cabaña Los Cipreses (12)
(56, 'http://localhost:8080/canonical-lodging-images/lodging-012-cabana-los-cipreses/masters/lodging-012-scene-01-exterior-cipreses-v001.jpg', 'Los Cipreses - Interior con vista', 12),
(57, 'http://localhost:8080/canonical-lodging-images/lodging-012-cabana-los-cipreses/masters/lodging-012-scene-02-dormitorio-principal-v001.jpg', 'Los Cipreses - Exterior', 12),
(58, 'http://localhost:8080/canonical-lodging-images/lodging-012-cabana-los-cipreses/masters/lodging-012-scene-03-bano-privado-v004.jpg', 'Los Cipreses - Fogón exterior', 12),
(59, 'http://localhost:8080/canonical-lodging-images/lodging-012-cabana-los-cipreses/masters/lodging-012-scene-04-estar-cocina-v001.jpg', 'Los Cipreses - Entorno natural', 12),
(60, 'http://localhost:8080/canonical-lodging-images/lodging-012-cabana-los-cipreses/masters/lodging-012-scene-05-jacuzzi-lago-v001.jpg', 'Los Cipreses - Baño', 12),
-- Departamento Nueva Córdoba (13)
(61, 'http://localhost:8080/canonical-lodging-images/lodging-013-departamento-nueva-cordoba/masters/lodging-013-scene-01-fachada-residencial-balcon-v001.jpg', 'Nueva Córdoba - Cocina', 13),
(62, 'http://localhost:8080/canonical-lodging-images/lodging-013-departamento-nueva-cordoba/masters/lodging-013-scene-02-dormitorio-escritorio-v001.jpg', 'Nueva Córdoba - Living', 13),
(63, 'http://localhost:8080/canonical-lodging-images/lodging-013-departamento-nueva-cordoba/masters/lodging-013-scene-03-bano-funcional-v001.jpg', 'Nueva Córdoba - Dormitorio', 13),
(64, 'http://localhost:8080/canonical-lodging-images/lodging-013-departamento-nueva-cordoba/masters/lodging-013-scene-04-living-comedor-cocina-v001.jpg', 'Nueva Córdoba - Comedor', 13),
(65, 'http://localhost:8080/canonical-lodging-images/lodging-013-departamento-nueva-cordoba/masters/lodging-013-scene-05-balcon-urbano-v001.jpg', 'Nueva Córdoba - Vestidor', 13),
-- Gran Hotel Tucumán (14)
(66, 'http://localhost:8080/canonical-lodging-images/lodging-014-gran-hotel-tucuman/masters/lodging-014-scene-01-fachada-restaurada-v001.jpg', 'Gran Hotel - Fachada', 14),
(67, 'http://localhost:8080/canonical-lodging-images/lodging-014-gran-hotel-tucuman/masters/lodging-014-scene-02-habitacion-amplia-v001.jpg', 'Gran Hotel - Piscina', 14),
(68, 'http://localhost:8080/canonical-lodging-images/lodging-014-gran-hotel-tucuman/masters/lodging-014-scene-03-bano-actualizado-v001.jpg', 'Gran Hotel - Suite', 14),
(69, 'http://localhost:8080/canonical-lodging-images/lodging-014-gran-hotel-tucuman/masters/lodging-014-scene-04-desayuno-patio-lobby-v001.jpg', 'Gran Hotel - Baño', 14),
(70, 'http://localhost:8080/canonical-lodging-images/lodging-014-gran-hotel-tucuman/masters/lodging-014-scene-05-spa-contemporaneo-v001.jpg', 'Gran Hotel - Vista habitación', 14),
-- Cabaña El Bolsón (15)
(71, 'http://localhost:8080/canonical-lodging-images/lodging-015-cabana-el-bolson/masters/lodging-015-scene-01-exterior-cabana-v001.jpg', 'El Bolsón - Living con fogón', 15),
(72, 'http://localhost:8080/canonical-lodging-images/lodging-015-cabana-el-bolson/masters/lodging-015-scene-02-dormitorio-bosque-v001.jpg', 'El Bolsón - Interior acogedor', 15),
(73, 'http://localhost:8080/canonical-lodging-images/lodging-015-cabana-el-bolson/masters/lodging-015-scene-03-bano-microcemento-v001.jpg', 'El Bolsón - Habitación', 15),
(74, 'http://localhost:8080/canonical-lodging-images/lodging-015-cabana-el-bolson/masters/lodging-015-scene-04-estar-cocina-hogar-v001.jpg', 'El Bolsón - Sala de estar', 15),
(75, 'http://localhost:8080/canonical-lodging-images/lodging-015-cabana-el-bolson/masters/lodging-015-scene-05-fogon-jardin-v001.jpg', 'El Bolsón - Baño', 15),
-- Departamento Chacras de Coria (16)
(76, 'http://localhost:8080/canonical-lodging-images/lodging-016-departamento-chacras-de-coria/masters/lodging-016-scene-01-complejo-bajo-jardines-v001.jpg', 'Chacras - Dormitorio', 16),
(77, 'http://localhost:8080/canonical-lodging-images/lodging-016-departamento-chacras-de-coria/masters/lodging-016-scene-02-dormitorio-terraza-v001.jpg', 'Chacras - Baño', 16),
(78, 'http://localhost:8080/canonical-lodging-images/lodging-016-departamento-chacras-de-coria/masters/lodging-016-scene-03-bano-mineral-v001.jpg', 'Chacras - Cocina', 16),
(79, 'http://localhost:8080/canonical-lodging-images/lodging-016-departamento-chacras-de-coria/masters/lodging-016-scene-04-living-comedor-cocina-v001.jpg', 'Chacras - Terraza', 16),
(80, 'http://localhost:8080/canonical-lodging-images/lodging-016-departamento-chacras-de-coria/masters/lodging-016-scene-05-terraza-piscina-compartida-v001.jpg', 'Chacras - Living', 16),
-- Hotel del Glaciar (17)
(81, 'http://localhost:8080/canonical-lodging-images/lodging-017-hotel-del-glaciar/masters/lodging-017-scene-01-exterior-escalonado-v001.jpg', 'Glaciar - Fachada', 17),
(82, 'http://localhost:8080/canonical-lodging-images/lodging-017-hotel-del-glaciar/masters/lodging-017-scene-02-habitacion-canal-beagle-v001.jpg', 'Glaciar - Habitación', 17),
(83, 'http://localhost:8080/canonical-lodging-images/lodging-017-hotel-del-glaciar/masters/lodging-017-scene-03-bano-privado-v001.jpg', 'Glaciar - Baño', 17),
(84, 'http://localhost:8080/canonical-lodging-images/lodging-017-hotel-del-glaciar/masters/lodging-017-scene-04-restaurante-fueguino-v001.jpg', 'Glaciar - Vista canal', 17),
(85, 'http://localhost:8080/canonical-lodging-images/lodging-017-hotel-del-glaciar/masters/lodging-017-scene-05-lounge-panoramico-v001.jpg', 'Glaciar - Suite', 17),
-- Cabaña La Cumbrecita (18)
(86, 'http://localhost:8080/canonical-lodging-images/lodging-018-cabana-san-roque/masters/lodging-018-scene-01-exterior-jardin-v001.jpg', 'Cumbrecita - Interior chimenea', 18),
(87, 'http://localhost:8080/canonical-lodging-images/lodging-018-cabana-san-roque/masters/lodging-018-scene-02-dormitorio-principal-v001.jpg', 'Cumbrecita - Vista lago', 18),
(88, 'http://localhost:8080/canonical-lodging-images/lodging-018-cabana-san-roque/masters/lodging-018-scene-03-bano-microcemento-v001.jpg', 'Cumbrecita - Habitación', 18),
(89, 'http://localhost:8080/canonical-lodging-images/lodging-018-cabana-san-roque/masters/lodging-018-scene-04-estar-cocina-comedor-v001.jpg', 'Cumbrecita - Baño', 18),
(90, 'http://localhost:8080/canonical-lodging-images/lodging-018-cabana-san-roque/masters/lodging-018-scene-05-muelle-privado-v001.jpg', 'Cumbrecita - Exterior', 18),
-- Departamento Belgrano (19)
(91, 'http://localhost:8080/canonical-lodging-images/lodging-019-departamento-belgrano/masters/lodging-019-scene-01-fachada-residencial-v001.jpg', 'Belgrano - Living', 19),
(92, 'http://localhost:8080/canonical-lodging-images/lodging-019-departamento-belgrano/masters/lodging-019-scene-02-dormitorio-principal-v001.jpg', 'Belgrano - Dormitorio', 19),
(93, 'http://localhost:8080/canonical-lodging-images/lodging-019-departamento-belgrano/masters/lodging-019-scene-03-bano-familiar-v001.jpg', 'Belgrano - Comedor', 19),
(94, 'http://localhost:8080/canonical-lodging-images/lodging-019-departamento-belgrano/masters/lodging-019-scene-04-living-comedor-cocina-v001.jpg', 'Belgrano - Vestidor', 19),
(95, 'http://localhost:8080/canonical-lodging-images/lodging-019-departamento-belgrano/masters/lodging-019-scene-05-balcon-corrido-equipado-v002.jpg', 'Belgrano - Balcón', 19),
-- Hotel La Perla (20)
(96, 'http://localhost:8080/canonical-lodging-images/lodging-020-hotel-la-perla/masters/lodging-020-scene-01-fachada-curva-la-perla-v001.jpg', 'La Perla - Habitación mar', 20),
(97, 'http://localhost:8080/canonical-lodging-images/lodging-020-hotel-la-perla/masters/lodging-020-scene-02-habitacion-balcon-semicircular-v001.jpg', 'La Perla - Baño', 20),
(98, 'http://localhost:8080/canonical-lodging-images/lodging-020-hotel-la-perla/masters/lodging-020-scene-03-bano-privado-coral-v001.jpg', 'La Perla - Piscina', 20),
(99, 'http://localhost:8080/canonical-lodging-images/lodging-020-hotel-la-perla/masters/lodging-020-scene-04-restaurante-costero-v001.jpg', 'La Perla - Suite', 20),
(100, 'http://localhost:8080/canonical-lodging-images/lodging-020-hotel-la-perla/masters/lodging-020-scene-05-piscina-spa-patio-v001.jpg', 'La Perla - Fachada', 20),
-- Cabaña Lacar (21)
(101, 'http://localhost:8080/canonical-lodging-images/lodging-021-cabana-lacar/masters/lodging-021-scene-01-exterior-desde-la-costa-v001.jpg', 'Lacar - Interior acogedor', 21),
(102, 'http://localhost:8080/canonical-lodging-images/lodging-021-cabana-lacar/masters/lodging-021-scene-02-dormitorio-principal-v001.jpg', 'Lacar - Vista lago', 21),
(103, 'http://localhost:8080/canonical-lodging-images/lodging-021-cabana-lacar/masters/lodging-021-scene-03-bano-privado-v001.jpg', 'Lacar - Sala de estar', 21),
(104, 'http://localhost:8080/canonical-lodging-images/lodging-021-cabana-lacar/masters/lodging-021-scene-04-pabellon-social-v001.jpg', 'Lacar - Baño', 21),
(105, 'http://localhost:8080/canonical-lodging-images/lodging-021-cabana-lacar/masters/lodging-021-scene-05-terraza-sendero-kayaks-v001.jpg', 'Lacar - Exterior', 21),
-- Departamento Pichincha (22)
(106, 'http://localhost:8080/canonical-lodging-images/lodging-022-departamento-pichincha/masters/lodging-022-scene-01-fachada-reciclada-v001.jpg', 'Pichincha - Baño', 22),
(107, 'http://localhost:8080/canonical-lodging-images/lodging-022-departamento-pichincha/masters/lodging-022-scene-02-dormitorio-ventana-alta-v001.jpg', 'Pichincha - Cocina', 22),
(108, 'http://localhost:8080/canonical-lodging-images/lodging-022-departamento-pichincha/masters/lodging-022-scene-03-bano-gris-grafito-v001.jpg', 'Pichincha - Balcón', 22),
(109, 'http://localhost:8080/canonical-lodging-images/lodging-022-departamento-pichincha/masters/lodging-022-scene-04-living-comedor-cocina-v001.jpg', 'Pichincha - Living', 22),
(110, 'http://localhost:8080/canonical-lodging-images/lodging-022-departamento-pichincha/masters/lodging-022-scene-05-terraza-urbana-parrilla-v001.jpg', 'Pichincha - Dormitorio', 22),
-- Hotel Cataratas Iguazú (23)
(111, 'http://localhost:8080/canonical-lodging-images/lodging-023-hotel-cataratas-iguazu/masters/lodging-023-scene-01-exterior-entre-vegetacion-v001.jpg', 'Cataratas - Habitación', 23),
(112, 'http://localhost:8080/canonical-lodging-images/lodging-023-hotel-cataratas-iguazu/masters/lodging-023-scene-02-habitacion-hacia-la-selva-v001.jpg', 'Cataratas - Piscina', 23),
(113, 'http://localhost:8080/canonical-lodging-images/lodging-023-hotel-cataratas-iguazu/masters/lodging-023-scene-03-bano-privado-mineral-v001.jpg', 'Cataratas - Restaurante', 23),
(114, 'http://localhost:8080/canonical-lodging-images/lodging-023-hotel-cataratas-iguazu/masters/lodging-023-scene-04-restaurante-regional-v001.jpg', 'Cataratas - Suite', 23),
(115, 'http://localhost:8080/canonical-lodging-images/lodging-023-hotel-cataratas-iguazu/masters/lodging-023-scene-05-piscina-infinita-selva-v001.jpg', 'Cataratas - Fachada', 23),
-- Cabaña Los Glaciares (24)
(116, 'http://localhost:8080/canonical-lodging-images/lodging-024-cabana-los-glaciares/masters/lodging-024-scene-01-exterior-lago-argentino-v003.jpg', 'Glaciares - Exterior', 24),
(117, 'http://localhost:8080/canonical-lodging-images/lodging-024-cabana-los-glaciares/masters/lodging-024-scene-02-dormitorio-principal-v001.jpg', 'Glaciares - Fogón', 24),
(118, 'http://localhost:8080/canonical-lodging-images/lodging-024-cabana-los-glaciares/masters/lodging-024-scene-03-bano-accesible-v001.jpg', 'Glaciares - Entorno', 24),
(119, 'http://localhost:8080/canonical-lodging-images/lodging-024-cabana-los-glaciares/masters/lodging-024-scene-04-estar-comedor-cocina-v001.jpg', 'Glaciares - Baño', 24),
(120, 'http://localhost:8080/canonical-lodging-images/lodging-024-cabana-los-glaciares/masters/lodging-024-scene-05-jacuzzi-patio-hundido-v001.jpg', 'Glaciares - Interior', 24),
-- Departamento Recoleta (25)
(121, 'http://localhost:8080/canonical-lodging-images/lodging-025-departamento-recoleta/masters/lodging-025-scene-01-fachada-historica-balcon-v002.jpg', 'Recoleta - Dormitorio', 25),
(122, 'http://localhost:8080/canonical-lodging-images/lodging-025-departamento-recoleta/masters/lodging-025-scene-02-dormitorio-principal-v001.jpg', 'Recoleta - Comedor', 25),
(123, 'http://localhost:8080/canonical-lodging-images/lodging-025-departamento-recoleta/masters/lodging-025-scene-03-bano-actualizado-v001.jpg', 'Recoleta - Vestidor', 25),
(124, 'http://localhost:8080/canonical-lodging-images/lodging-025-departamento-recoleta/masters/lodging-025-scene-04-living-comedor-cocina-v001.jpg', 'Recoleta - Vista exterior', 25),
(125, 'http://localhost:8080/canonical-lodging-images/lodging-025-departamento-recoleta/masters/lodging-025-scene-05-balcon-hierro-negro-v002.jpg', 'Recoleta - Living', 25),
-- Hotel Legado Mítico (26)
(126, 'http://localhost:8080/canonical-lodging-images/lodging-026-hotel-legado-mitico/masters/lodging-026-scene-01-fachada-porton-algarrobo-v001.jpg', 'Legado - Patio colonial', 26),
(127, 'http://localhost:8080/canonical-lodging-images/lodging-026-hotel-legado-mitico/masters/lodging-026-scene-02-habitacion-galeria-v001.jpg', 'Legado - Piscina', 26),
(128, 'http://localhost:8080/canonical-lodging-images/lodging-026-hotel-legado-mitico/masters/lodging-026-scene-03-bano-privado-v001.jpg', 'Legado - Suite', 26),
(129, 'http://localhost:8080/canonical-lodging-images/lodging-026-hotel-legado-mitico/masters/lodging-026-scene-04-restaurante-gourmet-v001.jpg', 'Legado - Fachada', 26),
(130, 'http://localhost:8080/canonical-lodging-images/lodging-026-hotel-legado-mitico/masters/lodging-026-scene-05-patio-principal-galeria-v001.jpg', 'Legado - Habitación', 26),
-- Hostel La Plata Centro (27)
(131, 'http://localhost:8080/canonical-lodging-images/lodging-027-hostel-la-plata-centro/masters/lodging-027-scene-01-fachada-urbana-reciclada-v001.jpg', 'La Plata - Sala común', 27),
(132, 'http://localhost:8080/canonical-lodging-images/lodging-027-hostel-la-plata-centro/masters/lodging-027-scene-02-dormitorio-compartido-v001.jpg', 'La Plata - Dormitorio', 27),
(133, 'http://localhost:8080/canonical-lodging-images/lodging-027-hostel-la-plata-centro/masters/lodging-027-scene-03-bano-compartido-v001.jpg', 'La Plata - Baño', 27),
(134, 'http://localhost:8080/canonical-lodging-images/lodging-027-hostel-la-plata-centro/masters/lodging-027-scene-04-cocina-comedor-estudio-v001.jpg', 'La Plata - Cocina', 27),
(135, 'http://localhost:8080/canonical-lodging-images/lodging-027-hostel-la-plata-centro/masters/lodging-027-scene-05-patio-trasero-parrilla-v001.jpg', 'La Plata - Fachada', 27),
-- Hostel Patagónico (28)
(136, 'http://localhost:8080/canonical-lodging-images/lodging-028-hostel-patagonico/masters/lodging-028-scene-01-fachada-acceso-protegido-v001.jpg', 'Patagónico - Habitación', 28),
(137, 'http://localhost:8080/canonical-lodging-images/lodging-028-hostel-patagonico/masters/lodging-028-scene-02-dormitorio-compartido-v001.jpg', 'Patagónico - Sala', 28),
(138, 'http://localhost:8080/canonical-lodging-images/lodging-028-hostel-patagonico/masters/lodging-028-scene-03-bano-compartido-v001.jpg', 'Patagónico - Habitación privada', 28),
(139, 'http://localhost:8080/canonical-lodging-images/lodging-028-hostel-patagonico/masters/lodging-028-scene-04-cocina-comedor-v001.jpg', 'Patagónico - Patio', 28),
(140, 'http://localhost:8080/canonical-lodging-images/lodging-028-hostel-patagonico/masters/lodging-028-scene-05-deposito-tecnico-terraza-v001.jpg', 'Patagónico - Fachada', 28),
-- Hostel San Telmo (29)
(141, 'http://localhost:8080/canonical-lodging-images/lodging-029-hostel-san-telmo/masters/lodging-029-scene-01-fachada-restaurada-v001.jpg', 'San Telmo - Baño', 29),
(142, 'http://localhost:8080/canonical-lodging-images/lodging-029-hostel-san-telmo/masters/lodging-029-scene-02-dormitorio-compartido-v001.jpg', 'San Telmo - Cocina', 29),
(143, 'http://localhost:8080/canonical-lodging-images/lodging-029-hostel-san-telmo/masters/lodging-029-scene-03-bano-compartido-v001.jpg', 'San Telmo - Fachada', 29),
(144, 'http://localhost:8080/canonical-lodging-images/lodging-029-hostel-san-telmo/masters/lodging-029-scene-04-cocina-comedor-patio-v001.jpg', 'San Telmo - Sala común', 29),
(145, 'http://localhost:8080/canonical-lodging-images/lodging-029-hostel-san-telmo/masters/lodging-029-scene-05-terraza-atardecer-v001.jpg', 'San Telmo - Dormitorio', 29),
-- Hostel Las Viñas (30)
(146, 'http://localhost:8080/canonical-lodging-images/lodging-030-hostel-las-vinas/masters/lodging-030-scene-01-fachada-urbana-v001.jpg', 'Las Viñas - Habitación privada', 30),
(147, 'http://localhost:8080/canonical-lodging-images/lodging-030-hostel-las-vinas/masters/lodging-030-scene-02-dormitorio-compartido-v001.jpg', 'Las Viñas - Patio', 30),
(148, 'http://localhost:8080/canonical-lodging-images/lodging-030-hostel-las-vinas/masters/lodging-030-scene-03-bano-compartido-v001.jpg', 'Las Viñas - Fachada', 30),
(149, 'http://localhost:8080/canonical-lodging-images/lodging-030-hostel-las-vinas/masters/lodging-030-scene-04-cocina-comedor-v001.jpg', 'Las Viñas - Habitación', 30),
(150, 'http://localhost:8080/canonical-lodging-images/lodging-030-hostel-las-vinas/masters/lodging-030-scene-05-patio-pergola-piscina-v001.jpg', 'Las Viñas - Sala', 30);

-- Features for lodging IDs 11–30
INSERT INTO lodging_features (lodging_id, feature_id) VALUES
(11, 1), (11, 3), (11, 7), (11, 5), (11, 2),
(12, 1), (12, 2), (12, 6), (12, 5),
(13, 1), (13, 7), (13, 8), (13, 2),
(14, 1), (14, 3), (14, 4), (14, 5), (14, 7),
(15, 1), (15, 2), (15, 6), (15, 4),
(16, 1), (16, 7), (16, 8), (16, 5), (16, 2),
(17, 1), (17, 3), (17, 7), (17, 4),
(18, 1), (18, 2), (18, 6), (18, 5),
(19, 1), (19, 7), (19, 8), (19, 3),
(20, 1), (20, 3), (20, 5), (20, 7), (20, 2),
(21, 1), (21, 2), (21, 6), (21, 5),
(22, 1), (22, 7), (22, 8), (22, 2),
(23, 1), (23, 3), (23, 5), (23, 7), (23, 2),
(24, 1), (24, 2), (24, 5),
(25, 1), (25, 7), (25, 8), (25, 3),
(26, 1), (26, 3), (26, 7), (26, 4),
(27, 1), (27, 4), (27, 2),
(28, 1), (28, 2), (28, 4),
(29, 1), (29, 4), (29, 8),
(30, 1), (30, 2), (30, 4), (30, 5);

-- Policies for lodging IDs 11–30
INSERT INTO lodging_policies (lodging_id, policy_id) VALUES
(11, 1), (11, 2), (11, 3), (11, 4),
(12, 1), (12, 2), (12, 3), (12, 5),
(13, 1), (13, 2), (13, 3), (13, 6),
(14, 1), (14, 2), (14, 3), (14, 4),
(15, 1), (15, 2), (15, 3), (15, 5),
(16, 1), (16, 2), (16, 3), (16, 4), (16, 6),
(17, 1), (17, 2), (17, 3), (17, 4),
(18, 1), (18, 2), (18, 3), (18, 5), (18, 6),
(19, 1), (19, 2), (19, 3), (19, 4),
(20, 1), (20, 2), (20, 3), (20, 4), (20, 6),
(21, 1), (21, 2), (21, 3), (21, 5),
(22, 1), (22, 2), (22, 3), (22, 6),
(23, 1), (23, 2), (23, 3), (23, 4),
(24, 1), (24, 2), (24, 3), (24, 5),
(25, 1), (25, 2), (25, 3), (25, 4),
(26, 1), (26, 2), (26, 3), (26, 4), (26, 6),
(27, 1), (27, 2), (27, 3), (27, 6),
(28, 1), (28, 2), (28, 3), (28, 6),
(29, 1), (29, 2), (29, 3), (29, 4), (29, 6),
(30, 1), (30, 2), (30, 3), (30, 6);

-- ============================================================
-- Sprint 4 seed extension (lodging IDs 31–38, resorts and glamping)
-- ============================================================

INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
-- Resorts (category_id = 5)
(31, 'Resort Iguazú Grand', 'Resort de lujo en la selva misionera a minutos de las Cataratas del Iguazú. Villas privadas con piscina desbordante, spa de tratamientos amazónicos y acceso privado al parque nacional. Todo incluido disponible.', 'Ruta 12 Km 1640', 'Puerto Iguazú', 'Argentina', '+54375890123', 'grand@resortiguazu.com', 5, 450.00, 4),
(32, 'Resort Termas Federación', 'Resort termal a orillas del lago Salto Grande. Acceso directo a las termas naturales, spa con circuito termal propio, restaurante gourmet y actividades náuticas en el lago. Ideal para familias y parejas.', 'Av. Las Termas 800', 'Federación', 'Argentina', '+54345567890', 'termas@resortfederacion.com', 5, 320.00, 6),
(33, 'Resort Valle de Uco', 'Resort vitivinícola entre viñedos y la cordillera de los Andes. Bodega propia, catas privadas, piscina infinity con vista a las montañas y acceso a excursiones de alta montaña. La experiencia de Mendoza premium.', 'Ruta Provincial 89 Km 14', 'Tunuyán', 'Argentina', '+54261890456', 'valleuco@resort.com', 5, 380.00, 4),
(34, 'Resort Llao Llao', 'Resort de montaña en la Patagonia andina con vista al lago Nahuel Huapi. Golf, esquí en invierno, kayak en verano y spa de primer nivel. El resort más icónico de la Argentina.', 'Av. Exequiel Bustillo Km 25', 'Bariloche', 'Argentina', '+54294478456', 'reservas@llaollao.com', 5, 520.00, 4),
-- Glamping (category_id = 6)
(35, 'Glamping Bosques del Sur', 'Domo geodésico en el corazón del bosque de arrayanes en Villa La Angostura. Cama king con vista directa al cielo estrellado, jacuzzi exterior, desayuno artesanal incluido. Sin señal celular, solo naturaleza.', 'Camino a Arrayanes Km 4', 'Villa La Angostura', 'Argentina', '+54294490789', 'bosques@glamping.com', 6, 280.00, 2),
(36, 'Glamping Cerro Chapelco', 'Tiendas de lujo frente al cerro Chapelco con vistas panorámicas a los Andes patagónicos. Chimenea, ropa de cama de calidad hotelera, guías de trekking y acceso a senderos exclusivos. Temporada verano e invierno.', 'Camino Chapelco Km 7', 'San Martín de los Andes', 'Argentina', '+54294460123', 'chapelco@glamping.com', 6, 240.00, 2),
(37, 'Glamping Valle Encantado', 'Carpas safari en el Valle Encantado, rodeadas de formaciones rocosas únicas y el río Limay. Plataforma elevada, baño privado con agua caliente, fogón y traslados desde Bariloche incluidos.', 'Valle Encantado, Ruta 237', 'Bariloche', 'Argentina', '+54294489012', 'valle@glamping.com', 6, 210.00, 3),
(38, 'Glamping Viñas del Sur', 'Domos de lujo entre viñedos orgánicos en Luján de Cuyo. Desayuno con productos de la finca, cata privada de vinos al atardecer, bicicletas para recorrer bodegas y piscina con vistas a los Andes. La experiencia glamping más sofisticada de Cuyo.', 'Finca El Sosneado, Carril Barriales 1800', 'Luján de Cuyo', 'Argentina', '+54261567890', 'vinas@glamping.com', 6, 260.00, 2);

-- Images for lodging IDs 31–38 (5 per lodging, image IDs 151–190)
INSERT INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Resort Iguazú Grand (31)
(151, 'http://localhost:8080/canonical-lodging-images/lodging-031-resort-iguazu-grand/masters/lodging-031-scene-01-pabellon-central-villas-selva-v001.jpg', 'Iguazú Grand - Fachada', 31),
(152, 'http://localhost:8080/canonical-lodging-images/lodging-031-resort-iguazu-grand/masters/lodging-031-scene-02-dormitorio-villa-patio-v001.jpg', 'Iguazú Grand - Suite', 31),
(153, 'http://localhost:8080/canonical-lodging-images/lodging-031-resort-iguazu-grand/masters/lodging-031-scene-03-bano-piedra-v001.jpg', 'Iguazú Grand - Baño', 31),
(154, 'http://localhost:8080/canonical-lodging-images/lodging-031-resort-iguazu-grand/masters/lodging-031-scene-04-restaurante-bar-v001.jpg', 'Iguazú Grand - Piscina', 31),
(155, 'http://localhost:8080/canonical-lodging-images/lodging-031-resort-iguazu-grand/masters/lodging-031-scene-05-piscina-infinita-spa-v001.jpg', 'Iguazú Grand - Villa', 31),
-- Resort Termas Federación (32)
(156, 'http://localhost:8080/canonical-lodging-images/lodging-032-resort-termas-federacion/masters/lodging-032-scene-01-complejo-jardines-piscinas-v001.jpg', 'Termas - Habitación', 32),
(157, 'http://localhost:8080/canonical-lodging-images/lodging-032-resort-termas-federacion/masters/lodging-032-scene-02-habitacion-familiar-v001.jpg', 'Termas - Piscina termal', 32),
(158, 'http://localhost:8080/canonical-lodging-images/lodging-032-resort-termas-federacion/masters/lodging-032-scene-03-bano-accesible-v001.jpg', 'Termas - Suite', 32),
(159, 'http://localhost:8080/canonical-lodging-images/lodging-032-resort-termas-federacion/masters/lodging-032-scene-04-restaurante-junto-al-lago-v001.jpg', 'Termas - Baño', 32),
(160, 'http://localhost:8080/canonical-lodging-images/lodging-032-resort-termas-federacion/masters/lodging-032-scene-05-circuito-termal-v001.jpg', 'Termas - Fachada', 32),
-- Resort Valle de Uco (33)
(161, 'http://localhost:8080/canonical-lodging-images/lodging-033-resort-valle-de-uco/masters/lodging-033-scene-01-pabellones-entre-vinedos-v002.jpg', 'Valle de Uco - Fachada', 33),
(162, 'http://localhost:8080/canonical-lodging-images/lodging-033-resort-valle-de-uco/masters/lodging-033-scene-02-habitacion-terraza-privada-v002.jpg', 'Valle de Uco - Habitación', 33),
(163, 'http://localhost:8080/canonical-lodging-images/lodging-033-resort-valle-de-uco/masters/lodging-033-scene-03-bano-mineral-v001.jpg', 'Valle de Uco - Piscina', 33),
(164, 'http://localhost:8080/canonical-lodging-images/lodging-033-resort-valle-de-uco/masters/lodging-033-scene-04-restaurante-degustacion-bodega-v002.jpg', 'Valle de Uco - Restaurante', 33),
(165, 'http://localhost:8080/canonical-lodging-images/lodging-033-resort-valle-de-uco/masters/lodging-033-scene-05-piscina-infinita-andes-v001.jpg', 'Valle de Uco - Vista viñedos', 33),
-- Resort Llao Llao (34)
(166, 'http://localhost:8080/canonical-lodging-images/lodging-034-resort-llao-llao/masters/lodging-034-scene-01-edificio-sobre-la-colina-v001.jpg', 'Llao Llao - Vista', 34),
(167, 'http://localhost:8080/canonical-lodging-images/lodging-034-resort-llao-llao/masters/lodging-034-scene-02-habitacion-panoramica-v001.jpg', 'Llao Llao - Suite', 34),
(168, 'http://localhost:8080/canonical-lodging-images/lodging-034-resort-llao-llao/masters/lodging-034-scene-03-bano-piedra-marmol-verde-v002.jpg', 'Llao Llao - Fachada', 34),
(169, 'http://localhost:8080/canonical-lodging-images/lodging-034-resort-llao-llao/masters/lodging-034-scene-04-restaurante-gran-salon-v001.jpg', 'Llao Llao - Habitación', 34),
(170, 'http://localhost:8080/canonical-lodging-images/lodging-034-resort-llao-llao/masters/lodging-034-scene-05-piscina-climatizada-interior-exterior-v001.jpg', 'Llao Llao - Piscina', 34),
-- Glamping Bosques del Sur (35)
(171, 'http://localhost:8080/canonical-lodging-images/lodging-035-glamping-bosques-del-sur/masters/lodging-035-scene-01-domo-y-pasarela-entre-los-arrayanes-v003.jpg', 'Bosques del Sur - Domo exterior', 35),
(172, 'http://localhost:8080/canonical-lodging-images/lodging-035-glamping-bosques-del-sur/masters/lodging-035-scene-02-cama-king-panoramica-v001.jpg', 'Bosques del Sur - Interior', 35),
(173, 'http://localhost:8080/canonical-lodging-images/lodging-035-glamping-bosques-del-sur/masters/lodging-035-scene-03-bano-privado-v001.jpg', 'Bosques del Sur - Vista bosque', 35),
(174, 'http://localhost:8080/canonical-lodging-images/lodging-035-glamping-bosques-del-sur/masters/lodging-035-scene-04-interior-y-rincon-de-desayuno-v002.jpg', 'Bosques del Sur - Cama', 35),
(175, 'http://localhost:8080/canonical-lodging-images/lodging-035-glamping-bosques-del-sur/masters/lodging-035-scene-05-jacuzzi-bajo-el-cielo-nocturno-v003.jpg', 'Bosques del Sur - Entorno', 35),
-- Glamping Cerro Chapelco (36)
(176, 'http://localhost:8080/canonical-lodging-images/lodging-036-glamping-cerro-chapelco/masters/lodging-036-scene-01-carpa-elevada-en-la-montana-v001.jpg', 'Chapelco - Carpa glamping', 36),
(177, 'http://localhost:8080/canonical-lodging-images/lodging-036-glamping-cerro-chapelco/masters/lodging-036-scene-02-cama-king-panoramica-v001.jpg', 'Chapelco - Interior', 36),
(178, 'http://localhost:8080/canonical-lodging-images/lodging-036-glamping-cerro-chapelco/masters/lodging-036-scene-03-bano-privado-aislado-v001.jpg', 'Chapelco - Vista cerro', 36),
(179, 'http://localhost:8080/canonical-lodging-images/lodging-036-glamping-cerro-chapelco/masters/lodging-036-scene-04-interior-con-hogar-y-desayuno-v001.jpg', 'Chapelco - Sala de estar', 36),
(180, 'http://localhost:8080/canonical-lodging-images/lodging-036-glamping-cerro-chapelco/masters/lodging-036-scene-05-vestibulo-tecnico-y-terraza-v003.jpg', 'Chapelco - Baño', 36),
-- Glamping Valle Encantado (37)
(181, 'http://localhost:8080/canonical-lodging-images/lodging-037-glamping-valle-encantado/masters/lodging-037-scene-01-carpa-accesible-entre-formaciones-rocosas-v002.jpg', 'Valle Encantado - Carpa safari', 37),
(182, 'http://localhost:8080/canonical-lodging-images/lodging-037-glamping-valle-encantado/masters/lodging-037-scene-02-area-de-descanso-panoramica-v003.jpg', 'Valle Encantado - Entorno rocoso', 37),
(183, 'http://localhost:8080/canonical-lodging-images/lodging-037-glamping-valle-encantado/masters/lodging-037-scene-03-bano-privado-accesible-v002.jpg', 'Valle Encantado - Fogón', 37),
(184, 'http://localhost:8080/canonical-lodging-images/lodging-037-glamping-valle-encantado/masters/lodging-037-scene-04-interior-con-comedor-y-lounge-v004.jpg', 'Valle Encantado - Naturaleza', 37),
(185, 'http://localhost:8080/canonical-lodging-images/lodging-037-glamping-valle-encantado/masters/lodging-037-scene-05-fogon-frente-al-valle-v001.jpg', 'Valle Encantado - Vista río', 37),
-- Glamping Viñas del Sur (38)
(186, 'http://localhost:8080/canonical-lodging-images/lodging-038-glamping-vinas-del-sur/masters/lodging-038-scene-01-domo-entre-vinedos-v003.jpg', 'Viñas del Sur - Domo viñedo', 38),
(187, 'http://localhost:8080/canonical-lodging-images/lodging-038-glamping-vinas-del-sur/masters/lodging-038-scene-02-v001.jpg', 'Viñas del Sur - Entorno', 38),
(188, 'http://localhost:8080/canonical-lodging-images/lodging-038-glamping-vinas-del-sur/masters/lodging-038-scene-03-v001.jpg', 'Viñas del Sur - Vista Andes', 38),
(189, 'http://localhost:8080/canonical-lodging-images/lodging-038-glamping-vinas-del-sur/masters/lodging-038-scene-04-v001.jpg', 'Viñas del Sur - Interior', 38),
(190, 'http://localhost:8080/canonical-lodging-images/lodging-038-glamping-vinas-del-sur/masters/lodging-038-scene-05-v001.jpg', 'Viñas del Sur - Exterior', 38);

-- Features for lodging IDs 31–38
INSERT INTO lodging_features (lodging_id, feature_id) VALUES
(31, 1), (31, 3), (31, 5), (31, 7), (31, 2),
(32, 1), (32, 3), (32, 5), (32, 6), (32, 2),
(33, 1), (33, 3), (33, 5), (33, 7), (33, 2),
(34, 1), (34, 3), (34, 5), (34, 7), (34, 2),
(35, 1), (35, 2), (35, 6),
(36, 1), (36, 2), (36, 5),
(37, 1), (37, 2), (37, 6),
(38, 1), (38, 2), (38, 5);

-- Policies for lodging IDs 31–38
INSERT INTO lodging_policies (lodging_id, policy_id) VALUES
(31, 1), (31, 2), (31, 3), (31, 4),
(32, 1), (32, 2), (32, 3), (32, 4), (32, 6),
(33, 1), (33, 2), (33, 3), (33, 4),
(34, 1), (34, 2), (34, 3), (34, 4),
(35, 1), (35, 2), (35, 3), (35, 5),
(36, 1), (36, 2), (36, 3), (36, 5),
(37, 1), (37, 2), (37, 3), (37, 5),
(38, 1), (38, 2), (38, 3), (38, 5);
