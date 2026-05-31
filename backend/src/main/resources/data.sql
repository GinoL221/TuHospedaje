-- ============================================================
-- Seed data for TuHospedaje — Sprint 3
-- ============================================================

-- Categorías
INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Hoteles', 'Hoteles urbanos y de negocios'),
(2, 'Cabañas', 'Cabañas rústicas en la naturaleza'),
(3, 'Departamentos', 'Departamentos céntricos totalmente equipados'),
(4, 'Hostels', 'Hostels económicos y sociales');

-- Características (amenities)
INSERT IGNORE INTO features (id, name, icon) VALUES
(1, 'WiFi gratis', 'fa-solid fa-wifi'),
(2, 'Estacionamiento', 'fa-solid fa-car'),
(3, 'Aire acondicionado', 'fa-solid fa-snowflake'),
(4, 'Desayuno incluido', 'fa-solid fa-utensils'),
(5, 'Pileta', 'fa-solid fa-water'),
(6, 'Mascotas permitidas', 'fa-solid fa-dog'),
(7, 'TV', 'fa-solid fa-tv'),
(8, 'Cocina equipada', 'fa-solid fa-kitchen-set');

-- Políticas
INSERT IGNORE INTO policies (id, name, description, icon) VALUES
(1, 'Check-in', 'A partir de las 14:00', 'fa-solid fa-clock'),
(2, 'Check-out', 'Hasta las 11:00', 'fa-solid fa-clock'),
(3, 'Cancelación', 'Cancelación gratuita hasta 48 horas antes del check-in', 'fa-solid fa-ban'),
(4, 'Fumadores', 'No se permite fumar en las habitaciones', 'fa-solid fa-smoking-ban'),
(5, 'Mascotas', 'Mascotas pequeñas permitidas con cargo adicional', 'fa-solid fa-dog'),
(6, 'Fiestas', 'No se permiten fiestas ni eventos', 'fa-solid fa-gift');

-- Alojamientos
INSERT IGNORE INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
(1, 'Hotel Buenos Aires Centro', 'Hotel céntrico con vista al obelisco. Habitaciones amplias y modernas con baño privado, TV LED y aire acondicionado. Cuenta con restaurante, bar y sala de negocios.', 'Av. Corrientes 1234', 'Buenos Aires', 'Argentina', '+54111234567', 'centro@hotelba.com', 1, 150.00, 4),
(2, 'Cabaña Los Arrayanes', 'Hermosa cabaña de montaña con vista al lago. Rodeada de bosques nativos, ideal para desconectarse. Incluye chimenea, hidromasaje y fogón exterior.', 'Ruta 40 Km 2050', 'Bariloche', 'Argentina', '+54294456789', 'arrayanes@cabanas.com', 2, 200.00, 6),
(3, 'Departamento Palermo Soho', 'Moderno departamento en el barrio más trendy de Buenos Aires. Cerca de bares, restaurantes y tiendas de diseño. Cuenta con balcón, cocina completa y laundry.', 'Gurruchaga 2100', 'Buenos Aires', 'Argentina', '+541198765432', 'palermo@departamento.com', 3, 180.00, 4),
(4, 'Hostel Córdoba Backpackers', 'Hostel para mochileros con ambiente internacional. Incluye desayuno, WiFi, y actividades grupales. Habitaciones compartidas y privadas.', 'Av. Recta Martinoli 5432', 'Córdoba', 'Argentina', '+54351123456', 'backpackers@hostelcba.com', 4, 50.00, 8),
(5, 'Cabaña del Lago', 'Cabaña frente al lago Nahuel Huapi con muelle privado. Ideal para parejas. Incluye kayaks y bicicletas de cortesía.', 'Av. Bustillo 4500', 'Bariloche', 'Argentina', '+54294432123', 'lago@cabanas.com', 2, 250.00, 4),
(6, 'Hotel Internacional', 'Hotel 4 estrellas en pleno centro de Mendoza. Cerca de bodegas y plazas. Pileta climatizada, spa y restaurante gourmet.', 'Av. San Martín 850', 'Mendoza', 'Argentina', '+54261456789', 'internacional@hotelmza.com', 1, 220.00, 4);

-- Imágenes
INSERT IGNORE INTO lodging_images (id, url, title, lodging_id) VALUES
(1, 'https://images.unsplash.com/photo-1566073771259-6a8506099945', 'Hotel Buenos Aires Centro - Fachada', 1),
(2, 'https://images.unsplash.com/photo-1582719508461-905c673771fd', 'Hotel Buenos Aires Centro - Habitación', 1),
(3, 'https://images.unsplash.com/photo-1590490359683-658d3d23f972', 'Cabaña Los Arrayanes - Exterior', 2),
(4, 'https://images.unsplash.com/photo-1518780664697-55e3ad937233', 'Cabaña Los Arrayanes - Interior', 2),
(5, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267', 'Departamento Palermo - Living', 3),
(6, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688', 'Departamento Palermo - Dormitorio', 3),
(7, 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5', 'Hostel Córdoba - Sala común', 4),
(8, 'https://images.unsplash.com/photo-1580587771525-78b9dba3b914', 'Cabaña del Lago - Vista', 5),
(9, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa', 'Hotel Internacional - Pileta', 6),
(10, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb', 'Hotel Internacional - Habitación', 6);

-- Relaciones lodging_features
INSERT IGNORE INTO lodging_features (lodging_id, feature_id) VALUES
(1, 1), (1, 3), (1, 7), (1, 2),
(2, 1), (2, 2), (2, 6), (2, 5),
(3, 1), (3, 3), (3, 7), (3, 8),
(4, 1), (4, 2), (4, 4),
(5, 1), (5, 2), (5, 5), (5, 6),
(6, 1), (6, 3), (6, 5), (6, 7), (6, 2);

-- Relaciones lodging_policies
INSERT IGNORE INTO lodging_policies (lodging_id, policy_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 2), (2, 3), (2, 5), (2, 6),
(3, 1), (3, 2), (3, 3), (3, 4),
(4, 1), (4, 2), (4, 3), (4, 6),
(5, 1), (5, 2), (5, 3), (5, 5),
(6, 1), (6, 2), (6, 3), (6, 4), (6, 6);
