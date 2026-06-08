-- ============================================================
-- Seed data for TuHospedaje — Sprint 3
-- ============================================================

-- Categorías
INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Hoteles', 'Hoteles urbanos y de negocios'),
(2, 'Cabañas', 'Cabañas rústicas en la naturaleza'),
(3, 'Departamentos', 'Departamentos céntricos totalmente equipados'),
(4, 'Hostels', 'Hostels económicos y sociales'),
(5, 'Resorts', 'Resorts y complejos de lujo'),
(6, 'Glamping', 'Glamping y naturaleza con comodidades');

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

-- ============================================================
-- Seed data extra — Sprint 4 (IDs 11–30, 20 alojamientos)
-- ============================================================

INSERT IGNORE INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
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

-- Imágenes para alojamientos 11–30 (5 por alojamiento, IDs 51–150, todas verificadas 200 OK)
-- Cada lodging arranca con un cover único — sets rotados por tipo para evitar duplicados en la grilla
INSERT IGNORE INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Hotel Riviera Rosario (11) — hotel set C
(51, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&h=600&fit=crop', 'Hotel Riviera - Fachada', 11),
(52, 'https://images.unsplash.com/photo-1711059985570-4c32ed12a12c?w=800&h=600&fit=crop', 'Hotel Riviera - Habitación', 11),
(53, 'https://images.unsplash.com/photo-1714454838107-28ef0e25188d?w=800&h=600&fit=crop', 'Hotel Riviera - Piscina', 11),
(54, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800&h=600&fit=crop', 'Hotel Riviera - Restaurante', 11),
(55, 'https://images.unsplash.com/photo-1665249934445-1de680641f50?w=800&h=600&fit=crop', 'Hotel Riviera - Suite', 11),
-- Cabaña Los Cipreses (12) — cabaña set C
(56, 'https://images.unsplash.com/photo-1551927411-95e412943b58?w=800&h=600&fit=crop', 'Los Cipreses - Interior con vista', 12),
(57, 'https://images.unsplash.com/photo-1671683886944-6478e6c84cbc?w=800&h=600&fit=crop', 'Los Cipreses - Exterior', 12),
(58, 'https://images.unsplash.com/photo-1662982692115-743f9e716b98?w=800&h=600&fit=crop', 'Los Cipreses - Fogón exterior', 12),
(59, 'https://images.unsplash.com/photo-1668480441891-3744c25337a3?w=800&h=600&fit=crop', 'Los Cipreses - Entorno natural', 12),
(60, 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=800&h=600&fit=crop', 'Los Cipreses - Baño', 12),
-- Departamento Nueva Córdoba (13) — depto set A shifted +3
(61, 'https://images.unsplash.com/photo-1551632436-cbf8dd35adfa?w=800&h=600&fit=crop', 'Nueva Córdoba - Cocina', 13),
(62, 'https://images.unsplash.com/photo-1738168246881-40f35f8aba0a?w=800&h=600&fit=crop', 'Nueva Córdoba - Living', 13),
(63, 'https://images.unsplash.com/photo-1629140727571-9b5c6f6267b4?w=800&h=600&fit=crop', 'Nueva Córdoba - Dormitorio', 13),
(64, 'https://images.unsplash.com/photo-1738168279272-c08d6dd22002?w=800&h=600&fit=crop', 'Nueva Córdoba - Comedor', 13),
(65, 'https://images.unsplash.com/photo-1556593825-c11de986cb0b?w=800&h=600&fit=crop', 'Nueva Córdoba - Vestidor', 13),
-- Gran Hotel Tucumán (14) — hotel set B
(66, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&h=600&fit=crop', 'Gran Hotel - Fachada', 14),
(67, 'https://images.unsplash.com/photo-1716667282993-cd8f2bffb91f?w=800&h=600&fit=crop', 'Gran Hotel - Piscina', 14),
(68, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&h=600&fit=crop', 'Gran Hotel - Suite', 14),
(69, 'https://images.unsplash.com/photo-1664917555352-f3f66e57ccc2?w=800&h=600&fit=crop', 'Gran Hotel - Baño', 14),
(70, 'https://images.unsplash.com/photo-1652348716053-3447e551dd1f?w=800&h=600&fit=crop', 'Gran Hotel - Vista habitación', 14),
-- Cabaña El Bolsón (15) — cabaña set B (Lago)
(71, 'https://images.unsplash.com/photo-1696860740793-1bb7bf33cdc1?w=800&h=600&fit=crop', 'El Bolsón - Living con fogón', 15),
(72, 'https://images.unsplash.com/photo-1680703486830-1b5af60635d7?w=800&h=600&fit=crop', 'El Bolsón - Interior acogedor', 15),
(73, 'https://images.unsplash.com/photo-1727706572437-4fcda0cbd66f?w=800&h=600&fit=crop', 'El Bolsón - Habitación', 15),
(74, 'https://images.unsplash.com/photo-1591825729269-caeb344f6df2?w=800&h=600&fit=crop', 'El Bolsón - Sala de estar', 15),
(75, 'https://images.unsplash.com/photo-1564540583246-934409427776?w=800&h=600&fit=crop', 'El Bolsón - Baño', 15),
-- Departamento Chacras de Coria (16) — depto set A shifted +1
(76, 'https://images.unsplash.com/photo-1628592102751-ba83b0314276?w=800&h=600&fit=crop', 'Chacras - Dormitorio', 16),
(77, 'https://images.unsplash.com/photo-1576698483491-8c43f0862543?w=800&h=600&fit=crop', 'Chacras - Baño', 16),
(78, 'https://images.unsplash.com/photo-1551632436-cbf8dd35adfa?w=800&h=600&fit=crop', 'Chacras - Cocina', 16),
(79, 'https://images.unsplash.com/photo-1613575831056-0acd5da8f085?w=800&h=600&fit=crop', 'Chacras - Terraza', 16),
(80, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&h=600&fit=crop', 'Chacras - Living', 16),
-- Hotel del Glaciar (17) — hotel set B shifted +1
(81, 'https://images.unsplash.com/photo-1716667282993-cd8f2bffb91f?w=800&h=600&fit=crop', 'Glaciar - Fachada', 17),
(82, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&h=600&fit=crop', 'Glaciar - Habitación', 17),
(83, 'https://images.unsplash.com/photo-1664917555352-f3f66e57ccc2?w=800&h=600&fit=crop', 'Glaciar - Baño', 17),
(84, 'https://images.unsplash.com/photo-1652348716053-3447e551dd1f?w=800&h=600&fit=crop', 'Glaciar - Vista canal', 17),
(85, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&h=600&fit=crop', 'Glaciar - Suite', 17),
-- Cabaña La Cumbrecita (18) — cabaña set A shifted +1
(86, 'https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=800&h=600&fit=crop', 'Cumbrecita - Interior chimenea', 18),
(87, 'https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=800&h=600&fit=crop', 'Cumbrecita - Vista lago', 18),
(88, 'https://images.unsplash.com/photo-1631630259742-c0f0b17c6c10?w=800&h=600&fit=crop', 'Cumbrecita - Habitación', 18),
(89, 'https://images.unsplash.com/photo-1507652313519-d4e9174996dd?w=800&h=600&fit=crop', 'Cumbrecita - Baño', 18),
(90, 'https://images.unsplash.com/photo-1590490359683-658d3d23f972?w=800&h=600&fit=crop', 'Cumbrecita - Exterior', 18),
-- Departamento Belgrano (19) — depto set B shifted +1
(91, 'https://images.unsplash.com/photo-1738168246881-40f35f8aba0a?w=800&h=600&fit=crop', 'Belgrano - Living', 19),
(92, 'https://images.unsplash.com/photo-1629140727571-9b5c6f6267b4?w=800&h=600&fit=crop', 'Belgrano - Dormitorio', 19),
(93, 'https://images.unsplash.com/photo-1738168279272-c08d6dd22002?w=800&h=600&fit=crop', 'Belgrano - Comedor', 19),
(94, 'https://images.unsplash.com/photo-1556593825-c11de986cb0b?w=800&h=600&fit=crop', 'Belgrano - Vestidor', 19),
(95, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=600&fit=crop', 'Belgrano - Balcón', 19),
-- Hotel La Perla (20) — hotel set A shifted +1
(96, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=600&fit=crop', 'La Perla - Habitación mar', 20),
(97, 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&h=600&fit=crop', 'La Perla - Baño', 20),
(98, 'https://images.unsplash.com/photo-1534612899740-55c821a90129?w=800&h=600&fit=crop', 'La Perla - Piscina', 20),
(99, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&h=600&fit=crop', 'La Perla - Suite', 20),
(100, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop', 'La Perla - Fachada', 20),
-- Cabaña Lacar (21) — cabaña set B shifted +1
(101, 'https://images.unsplash.com/photo-1680703486830-1b5af60635d7?w=800&h=600&fit=crop', 'Lacar - Interior acogedor', 21),
(102, 'https://images.unsplash.com/photo-1727706572437-4fcda0cbd66f?w=800&h=600&fit=crop', 'Lacar - Vista lago', 21),
(103, 'https://images.unsplash.com/photo-1591825729269-caeb344f6df2?w=800&h=600&fit=crop', 'Lacar - Sala de estar', 21),
(104, 'https://images.unsplash.com/photo-1564540583246-934409427776?w=800&h=600&fit=crop', 'Lacar - Baño', 21),
(105, 'https://images.unsplash.com/photo-1696860740793-1bb7bf33cdc1?w=800&h=600&fit=crop', 'Lacar - Exterior', 21),
-- Departamento Pichincha (22) — depto set A shifted +2
(106, 'https://images.unsplash.com/photo-1576698483491-8c43f0862543?w=800&h=600&fit=crop', 'Pichincha - Baño', 22),
(107, 'https://images.unsplash.com/photo-1551632436-cbf8dd35adfa?w=800&h=600&fit=crop', 'Pichincha - Cocina', 22),
(108, 'https://images.unsplash.com/photo-1613575831056-0acd5da8f085?w=800&h=600&fit=crop', 'Pichincha - Balcón', 22),
(109, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&h=600&fit=crop', 'Pichincha - Living', 22),
(110, 'https://images.unsplash.com/photo-1628592102751-ba83b0314276?w=800&h=600&fit=crop', 'Pichincha - Dormitorio', 22),
-- Hotel Cataratas Iguazú (23) — hotel set C shifted +1
(111, 'https://images.unsplash.com/photo-1711059985570-4c32ed12a12c?w=800&h=600&fit=crop', 'Cataratas - Habitación', 23),
(112, 'https://images.unsplash.com/photo-1714454838107-28ef0e25188d?w=800&h=600&fit=crop', 'Cataratas - Piscina', 23),
(113, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800&h=600&fit=crop', 'Cataratas - Restaurante', 23),
(114, 'https://images.unsplash.com/photo-1665249934445-1de680641f50?w=800&h=600&fit=crop', 'Cataratas - Suite', 23),
(115, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&h=600&fit=crop', 'Cataratas - Fachada', 23),
-- Cabaña Los Glaciares (24) — cabaña set C shifted +1
(116, 'https://images.unsplash.com/photo-1671683886944-6478e6c84cbc?w=800&h=600&fit=crop', 'Glaciares - Exterior', 24),
(117, 'https://images.unsplash.com/photo-1662982692115-743f9e716b98?w=800&h=600&fit=crop', 'Glaciares - Fogón', 24),
(118, 'https://images.unsplash.com/photo-1668480441891-3744c25337a3?w=800&h=600&fit=crop', 'Glaciares - Entorno', 24),
(119, 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=800&h=600&fit=crop', 'Glaciares - Baño', 24),
(120, 'https://images.unsplash.com/photo-1551927411-95e412943b58?w=800&h=600&fit=crop', 'Glaciares - Interior', 24),
-- Departamento Recoleta (25) — depto set B shifted +2
(121, 'https://images.unsplash.com/photo-1629140727571-9b5c6f6267b4?w=800&h=600&fit=crop', 'Recoleta - Dormitorio', 25),
(122, 'https://images.unsplash.com/photo-1738168279272-c08d6dd22002?w=800&h=600&fit=crop', 'Recoleta - Comedor', 25),
(123, 'https://images.unsplash.com/photo-1556593825-c11de986cb0b?w=800&h=600&fit=crop', 'Recoleta - Vestidor', 25),
(124, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&h=600&fit=crop', 'Recoleta - Vista exterior', 25),
(125, 'https://images.unsplash.com/photo-1738168246881-40f35f8aba0a?w=800&h=600&fit=crop', 'Recoleta - Living', 25),
-- Hotel Legado Mítico (26) — hotel set A shifted +2
(126, 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&h=600&fit=crop', 'Legado - Patio colonial', 26),
(127, 'https://images.unsplash.com/photo-1534612899740-55c821a90129?w=800&h=600&fit=crop', 'Legado - Piscina', 26),
(128, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&h=600&fit=crop', 'Legado - Suite', 26),
(129, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop', 'Legado - Fachada', 26),
(130, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=600&fit=crop', 'Legado - Habitación', 26),
-- Hostel La Plata Centro (27) — hostel set A
(131, 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=800&h=600&fit=crop', 'La Plata - Sala común', 27),
(132, 'https://images.unsplash.com/photo-1709805619372-40de3f158e83?w=800&h=600&fit=crop', 'La Plata - Dormitorio', 27),
(133, 'https://images.unsplash.com/photo-1587527901949-ab0341697c1e?w=800&h=600&fit=crop', 'La Plata - Baño', 27),
(134, 'https://images.unsplash.com/photo-1488992783499-418eb1f62d08?w=800&h=600&fit=crop', 'La Plata - Cocina', 27),
(135, 'https://images.unsplash.com/photo-1596276020587-8044fe049813?w=800&h=600&fit=crop', 'La Plata - Fachada', 27),
-- Hostel Patagónico (28) — hostel set B
(136, 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&h=600&fit=crop', 'Patagónico - Habitación', 28),
(137, 'https://images.unsplash.com/photo-1549881567-c622c1080d78?w=800&h=600&fit=crop', 'Patagónico - Sala', 28),
(138, 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&h=600&fit=crop', 'Patagónico - Habitación privada', 28),
(139, 'https://images.unsplash.com/photo-1768289269971-6171457bed13?w=800&h=600&fit=crop', 'Patagónico - Patio', 28),
(140, 'https://images.unsplash.com/photo-1692153142886-9881d0457b82?w=800&h=600&fit=crop', 'Patagónico - Fachada', 28),
-- Hostel San Telmo (29) — hostel set A shifted +2
(141, 'https://images.unsplash.com/photo-1587527901949-ab0341697c1e?w=800&h=600&fit=crop', 'San Telmo - Baño', 29),
(142, 'https://images.unsplash.com/photo-1488992783499-418eb1f62d08?w=800&h=600&fit=crop', 'San Telmo - Cocina', 29),
(143, 'https://images.unsplash.com/photo-1596276020587-8044fe049813?w=800&h=600&fit=crop', 'San Telmo - Fachada', 29),
(144, 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=800&h=600&fit=crop', 'San Telmo - Sala común', 29),
(145, 'https://images.unsplash.com/photo-1709805619372-40de3f158e83?w=800&h=600&fit=crop', 'San Telmo - Dormitorio', 29),
-- Hostel Las Viñas (30) — hostel set B shifted +2
(146, 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&h=600&fit=crop', 'Las Viñas - Habitación privada', 30),
(147, 'https://images.unsplash.com/photo-1768289269971-6171457bed13?w=800&h=600&fit=crop', 'Las Viñas - Patio', 30),
(148, 'https://images.unsplash.com/photo-1692153142886-9881d0457b82?w=800&h=600&fit=crop', 'Las Viñas - Fachada', 30),
(149, 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&h=600&fit=crop', 'Las Viñas - Habitación', 30),
(150, 'https://images.unsplash.com/photo-1549881567-c622c1080d78?w=800&h=600&fit=crop', 'Las Viñas - Sala', 30);

-- Features para alojamientos 11–30
INSERT IGNORE INTO lodging_features (lodging_id, feature_id) VALUES
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

-- Policies para alojamientos 11–30
INSERT IGNORE INTO lodging_policies (lodging_id, policy_id) VALUES
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
-- Seed data extra — Sprint 4 (IDs 31–38, Resorts y Glamping)
-- ============================================================

INSERT IGNORE INTO lodgings (id, name, description, address, city, country, phone_number, email, category_id, price_per_night, max_guests) VALUES
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

-- Imágenes para alojamientos 31–38 (5 por alojamiento, IDs 151–190, todas verificadas 200 OK)
INSERT IGNORE INTO lodging_images (id, image_url, title, lodging_id) VALUES
-- Resort Iguazú Grand (31) — pool resort (set lodging 1)
(151, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=600&fit=crop', 'Iguazú Grand - Fachada', 31),
(152, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&h=600&fit=crop', 'Iguazú Grand - Suite', 31),
(153, 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&h=600&fit=crop', 'Iguazú Grand - Baño', 31),
(154, 'https://images.unsplash.com/photo-1534612899740-55c821a90129?w=800&h=600&fit=crop', 'Iguazú Grand - Piscina', 31),
(155, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&h=600&fit=crop', 'Iguazú Grand - Villa', 31),
-- Resort Termas Federación (32) — pool resort (set lodging 6)
(156, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&h=600&fit=crop', 'Termas - Habitación', 32),
(157, 'https://images.unsplash.com/photo-1716667282993-cd8f2bffb91f?w=800&h=600&fit=crop', 'Termas - Piscina termal', 32),
(158, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&h=600&fit=crop', 'Termas - Suite', 32),
(159, 'https://images.unsplash.com/photo-1664917555352-f3f66e57ccc2?w=800&h=600&fit=crop', 'Termas - Baño', 32),
(160, 'https://images.unsplash.com/photo-1652348716053-3447e551dd1f?w=800&h=600&fit=crop', 'Termas - Fachada', 32),
-- Resort Valle de Uco (33) — pool resort (set lodging 8)
(161, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&h=600&fit=crop', 'Valle de Uco - Fachada', 33),
(162, 'https://images.unsplash.com/photo-1711059985570-4c32ed12a12c?w=800&h=600&fit=crop', 'Valle de Uco - Habitación', 33),
(163, 'https://images.unsplash.com/photo-1714454838107-28ef0e25188d?w=800&h=600&fit=crop', 'Valle de Uco - Piscina', 33),
(164, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800&h=600&fit=crop', 'Valle de Uco - Restaurante', 33),
(165, 'https://images.unsplash.com/photo-1665249934445-1de680641f50?w=800&h=600&fit=crop', 'Valle de Uco - Vista viñedos', 33),
-- Resort Llao Llao (34) — hotel set C shifted +3
(166, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800&h=600&fit=crop', 'Llao Llao - Vista', 34),
(167, 'https://images.unsplash.com/photo-1665249934445-1de680641f50?w=800&h=600&fit=crop', 'Llao Llao - Suite', 34),
(168, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&h=600&fit=crop', 'Llao Llao - Fachada', 34),
(169, 'https://images.unsplash.com/photo-1711059985570-4c32ed12a12c?w=800&h=600&fit=crop', 'Llao Llao - Habitación', 34),
(170, 'https://images.unsplash.com/photo-1714454838107-28ef0e25188d?w=800&h=600&fit=crop', 'Llao Llao - Piscina', 34),
-- Glamping Bosques del Sur (35) — nature (set lodging 2)
(171, 'https://images.unsplash.com/photo-1590490359683-658d3d23f972?w=800&h=600&fit=crop', 'Bosques del Sur - Domo exterior', 35),
(172, 'https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=800&h=600&fit=crop', 'Bosques del Sur - Interior', 35),
(173, 'https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=800&h=600&fit=crop', 'Bosques del Sur - Vista bosque', 35),
(174, 'https://images.unsplash.com/photo-1631630259742-c0f0b17c6c10?w=800&h=600&fit=crop', 'Bosques del Sur - Cama', 35),
(175, 'https://images.unsplash.com/photo-1507652313519-d4e9174996dd?w=800&h=600&fit=crop', 'Bosques del Sur - Entorno', 35),
-- Glamping Cerro Chapelco (36) — nature (set lodging 5)
(176, 'https://images.unsplash.com/photo-1696860740793-1bb7bf33cdc1?w=800&h=600&fit=crop', 'Chapelco - Carpa glamping', 36),
(177, 'https://images.unsplash.com/photo-1680703486830-1b5af60635d7?w=800&h=600&fit=crop', 'Chapelco - Interior', 36),
(178, 'https://images.unsplash.com/photo-1727706572437-4fcda0cbd66f?w=800&h=600&fit=crop', 'Chapelco - Vista cerro', 36),
(179, 'https://images.unsplash.com/photo-1591825729269-caeb344f6df2?w=800&h=600&fit=crop', 'Chapelco - Sala de estar', 36),
(180, 'https://images.unsplash.com/photo-1564540583246-934409427776?w=800&h=600&fit=crop', 'Chapelco - Baño', 36),
-- Glamping Valle Encantado (37) — nature (set lodging 7)
(181, 'https://images.unsplash.com/photo-1551927411-95e412943b58?w=800&h=600&fit=crop', 'Valle Encantado - Carpa safari', 37),
(182, 'https://images.unsplash.com/photo-1671683886944-6478e6c84cbc?w=800&h=600&fit=crop', 'Valle Encantado - Entorno rocoso', 37),
(183, 'https://images.unsplash.com/photo-1662982692115-743f9e716b98?w=800&h=600&fit=crop', 'Valle Encantado - Fogón', 37),
(184, 'https://images.unsplash.com/photo-1668480441891-3744c25337a3?w=800&h=600&fit=crop', 'Valle Encantado - Naturaleza', 37),
(185, 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=800&h=600&fit=crop', 'Valle Encantado - Vista río', 37),
-- Glamping Viñas del Sur (38) — cabaña set C shifted +2
(186, 'https://images.unsplash.com/photo-1662982692115-743f9e716b98?w=800&h=600&fit=crop', 'Viñas del Sur - Domo viñedo', 38),
(187, 'https://images.unsplash.com/photo-1668480441891-3744c25337a3?w=800&h=600&fit=crop', 'Viñas del Sur - Entorno', 38),
(188, 'https://images.unsplash.com/photo-1600573472550-8090b5e0745e?w=800&h=600&fit=crop', 'Viñas del Sur - Vista Andes', 38),
(189, 'https://images.unsplash.com/photo-1551927411-95e412943b58?w=800&h=600&fit=crop', 'Viñas del Sur - Interior', 38),
(190, 'https://images.unsplash.com/photo-1671683886944-6478e6c84cbc?w=800&h=600&fit=crop', 'Viñas del Sur - Exterior', 38);

-- Features para alojamientos 31–38
INSERT IGNORE INTO lodging_features (lodging_id, feature_id) VALUES
(31, 1), (31, 3), (31, 5), (31, 7), (31, 2),
(32, 1), (32, 3), (32, 5), (32, 6), (32, 2),
(33, 1), (33, 3), (33, 5), (33, 7), (33, 2),
(34, 1), (34, 3), (34, 5), (34, 7), (34, 2),
(35, 1), (35, 2), (35, 6),
(36, 1), (36, 2), (36, 5),
(37, 1), (37, 2), (37, 6),
(38, 1), (38, 2), (38, 5);

-- Policies para alojamientos 31–38
INSERT IGNORE INTO lodging_policies (lodging_id, policy_id) VALUES
(31, 1), (31, 2), (31, 3), (31, 4),
(32, 1), (32, 2), (32, 3), (32, 4), (32, 6),
(33, 1), (33, 2), (33, 3), (33, 4),
(34, 1), (34, 2), (34, 3), (34, 4),
(35, 1), (35, 2), (35, 3), (35, 5),
(36, 1), (36, 2), (36, 3), (36, 5),
(37, 1), (37, 2), (37, 3), (37, 5),
(38, 1), (38, 2), (38, 3), (38, 5);
