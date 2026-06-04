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
(1, 'WiFi gratis', 'wifi'),
(2, 'Estacionamiento', 'car'),
(3, 'Aire acondicionado', 'thermometer-snowflake'),
(4, 'Desayuno incluido', 'utensils'),
(5, 'Pileta', 'water'),
(6, 'Mascotas permitidas', 'paw-print'),
(7, 'TV', 'tv'),
(8, 'Cocina equipada', 'kitchen-set');

-- Políticas
INSERT IGNORE INTO policies (id, name, description, icon) VALUES
(1, 'Check-in', 'A partir de las 14:00', 'clock'),
(2, 'Check-out', 'Hasta las 11:00', 'clock'),
(3, 'Cancelación', 'Cancelación gratuita hasta 48 horas antes del check-in', 'ban'),
(4, 'Fumadores', 'No se permite fumar en las habitaciones', 'smoking-ban'),
(5, 'Mascotas', 'Mascotas pequeñas permitidas con cargo adicional', 'paw-print'),
(6, 'Fiestas', 'No se permiten fiestas ni eventos', 'party-popper');

-- Usuario admin (contraseña: Admin1)
INSERT IGNORE INTO users (id, first_name, last_name, email, password, role) VALUES
(1, 'Admin', 'TuHospedaje', 'admin@tuhospedaje.com', '$2a$10$/Di38qfA1eeuSbekhbf74OHF0a.gN.seovg9A7lKbY336bLp3bZnW', 'ADMIN');

-- Alojamientos
INSERT IGNORE INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
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

-- Imágenes (5 por alojamiento, todas Unsplash, todas verificadas 200 OK, 0 repeticiones)
INSERT IGNORE INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Hotel Buenos Aires Centro
(1, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop', 'Hotel Buenos Aires Centro - Fachada', 1),
(2, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=600&fit=crop', 'Hotel Buenos Aires Centro - Habitación', 1),
(3, 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&h=600&fit=crop', 'Hotel Buenos Aires Centro - Baño', 1),
(4, 'https://images.unsplash.com/photo-1534612899740-55c821a90129?w=800&h=600&fit=crop', 'Hotel Buenos Aires Centro - Pileta', 1),
(5, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&h=600&fit=crop', 'Hotel Buenos Aires Centro - Suite', 1),
-- Cabaña Los Arrayanes
(6, 'https://images.unsplash.com/photo-1590490359683-658d3d23f972?w=800&h=600&fit=crop', 'Cabaña Los Arrayanes - Exterior', 2),
(7, 'https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=800&h=600&fit=crop', 'Cabaña Los Arrayanes - Interior con chimenea', 2),
(8, 'https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=800&h=600&fit=crop', 'Cabaña Los Arrayanes - Vista al lago', 2),
(9, 'https://images.unsplash.com/photo-1631630259742-c0f0b17c6c10?w=800&h=600&fit=crop', 'Cabaña Los Arrayanes - Habitación', 2),
(10, 'https://images.unsplash.com/photo-1507652313519-d4e9174996dd?w=800&h=600&fit=crop', 'Cabaña Los Arrayanes - Baño', 2),
-- Departamento Palermo Soho
(11, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&h=600&fit=crop', 'Departamento Palermo - Living', 3),
(12, 'https://images.unsplash.com/photo-1628592102751-ba83b0314276?w=800&h=600&fit=crop', 'Departamento Palermo - Dormitorio', 3),
(13, 'https://images.unsplash.com/photo-1576698483491-8c43f0862543?w=800&h=600&fit=crop', 'Departamento Palermo - Baño', 3),
(14, 'https://images.unsplash.com/photo-1551632436-cbf8dd35adfa?w=800&h=600&fit=crop', 'Departamento Palermo - Cocina', 3),
(15, 'https://images.unsplash.com/photo-1613575831056-0acd5da8f085?w=800&h=600&fit=crop', 'Departamento Palermo - Living completo', 3),
-- Hostel Córdoba Backpackers
(16, 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=800&h=600&fit=crop', 'Hostel Córdoba - Sala común', 4),
(17, 'https://images.unsplash.com/photo-1709805619372-40de3f158e83?w=800&h=600&fit=crop', 'Hostel Córdoba - Dormitorio compartido', 4),
(18, 'https://images.unsplash.com/photo-1587527901949-ab0341697c1e?w=800&h=600&fit=crop', 'Hostel Córdoba - Baño', 4),
(19, 'https://images.unsplash.com/photo-1488992783499-418eb1f62d08?w=800&h=600&fit=crop', 'Hostel Córdoba - Cocina', 4),
(20, 'https://images.unsplash.com/photo-1596276020587-8044fe049813?w=800&h=600&fit=crop', 'Hostel Córdoba - Fachada', 4),
-- Cabaña del Lago
(21, 'https://images.unsplash.com/photo-1696860740793-1bb7bf33cdc1?w=800&h=600&fit=crop', 'Cabaña del Lago - Living con fogón', 5),
(22, 'https://images.unsplash.com/photo-1680703486830-1b5af60635d7?w=800&h=600&fit=crop', 'Cabaña del Lago - Interior acogedor', 5),
(23, 'https://images.unsplash.com/photo-1727706572437-4fcda0cbd66f?w=800&h=600&fit=crop', 'Cabaña del Lago - Habitación', 5),
(24, 'https://images.unsplash.com/photo-1591825729269-caeb344f6df2?w=800&h=600&fit=crop', 'Cabaña del Lago - Sala de estar', 5),
(25, 'https://images.unsplash.com/photo-1564540583246-934409427776?w=800&h=600&fit=crop', 'Cabaña del Lago - Baño', 5),
-- Hotel Internacional
(26, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&h=600&fit=crop', 'Hotel Internacional - Habitación', 6),
(27, 'https://images.unsplash.com/photo-1716667282993-cd8f2bffb91f?w=800&h=600&fit=crop', 'Hotel Internacional - Pileta', 6),
(28, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&h=600&fit=crop', 'Hotel Internacional - Suite', 6),
(29, 'https://images.unsplash.com/photo-1664917555352-f3f66e57ccc2?w=800&h=600&fit=crop', 'Hotel Internacional - Baño', 6),
(30, 'https://images.unsplash.com/photo-1652348716053-3447e551dd1f?w=800&h=600&fit=crop', 'Hotel Internacional - Fachada', 6),
-- Cabaña El Mirador
(31, 'https://images.unsplash.com/photo-1551927411-95e412943b58?w=800&h=600&fit=crop', 'Cabaña El Mirador - Interior con vista', 7),
(32, 'https://images.unsplash.com/photo-1671683886944-6478e6c84cbc?w=800&h=600&fit=crop', 'Cabaña El Mirador - Cartel del valle', 7),
(33, 'https://images.unsplash.com/photo-1662982692115-743f9e716b98?w=800&h=600&fit=crop', 'Cabaña El Mirador - Fogón exterior', 7),
(34, 'https://images.unsplash.com/photo-1668480441891-3744c25337a3?w=800&h=600&fit=crop', 'Cabaña El Mirador - Entorno natural', 7),
(35, 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=800&h=600&fit=crop', 'Cabaña El Mirador - Baño', 7),
-- Hotel Mar del Plata
(36, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&h=600&fit=crop', 'Hotel Mar del Plata - Fachada', 8),
(37, 'https://images.unsplash.com/photo-1711059985570-4c32ed12a12c?w=800&h=600&fit=crop', 'Hotel Mar del Plata - Habitación', 8),
(38, 'https://images.unsplash.com/photo-1714454838107-28ef0e25188d?w=800&h=600&fit=crop', 'Hotel Mar del Plata - Pileta', 8),
(39, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800&h=600&fit=crop', 'Hotel Mar del Plata - Restaurante', 8),
(40, 'https://images.unsplash.com/photo-1665249934445-1de680641f50?w=800&h=600&fit=crop', 'Hotel Mar del Plata - Habitación vista', 8),
-- Departamento Puerto Madero
(41, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=600&fit=crop', 'Puerto Madero - Vista al dique', 9),
(42, 'https://images.unsplash.com/photo-1738168246881-40f35f8aba0a?w=800&h=600&fit=crop', 'Puerto Madero - Living', 9),
(43, 'https://images.unsplash.com/photo-1629140727571-9b5c6f6267b4?w=800&h=600&fit=crop', 'Puerto Madero - Dormitorio', 9),
(44, 'https://images.unsplash.com/photo-1738168279272-c08d6dd22002?w=800&h=600&fit=crop', 'Puerto Madero - Sala comedor', 9),
(45, 'https://images.unsplash.com/photo-1556593825-c11de986cb0b?w=800&h=600&fit=crop', 'Puerto Madero - Vestidor', 9),
-- Hostel Salta Andino
(46, 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&h=600&fit=crop', 'Hostel Salta - Habitación', 10),
(47, 'https://images.unsplash.com/photo-1549881567-c622c1080d78?w=800&h=600&fit=crop', 'Hostel Salta - Sala de estar', 10),
(48, 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&h=600&fit=crop', 'Hostel Salta - Habitación privada', 10),
(49, 'https://images.unsplash.com/photo-1768289269971-6171457bed13?w=800&h=600&fit=crop', 'Hostel Salta - Patio', 10),
(50, 'https://images.unsplash.com/photo-1692153142886-9881d0457b82?w=800&h=600&fit=crop', 'Hostel Salta - Fachada', 10);

-- Relaciones lodging_features
INSERT IGNORE INTO lodging_features (lodging_id, feature_id) VALUES
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

-- Relaciones lodging_policies
INSERT IGNORE INTO lodging_policies (lodging_id, policy_id) VALUES
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
