-- Demo catalog. Stock mix: most in stock, two sold out (Refactoring, TDD by Example), one last copy (DDD).

INSERT INTO books (isbn, title, author, price) VALUES
    ('9780132350884', 'Clean Code',                            'Robert C. Martin',                  32.99),
    ('9780201633610', 'Design Patterns',                       'Erich Gamma et al.',                44.50),
    ('9780134757599', 'Refactoring',                           'Martin Fowler',                     42.00),
    ('9780321125217', 'Domain-Driven Design',                  'Eric Evans',                        49.95),
    ('9780135957059', 'The Pragmatic Programmer',              'David Thomas & Andrew Hunt',        39.99),
    ('9780321146533', 'Test Driven Development: By Example',   'Kent Beck',                         34.95),
    ('9780134494166', 'Clean Architecture',                    'Robert C. Martin',                  29.99),
    ('9781449373320', 'Designing Data-Intensive Applications', 'Martin Kleppmann',                  45.99),
    ('9780596007126', 'Head First Design Patterns',            'Eric Freeman & Elisabeth Robson',   38.00),
    ('9780137081073', 'The Clean Coder',                       'Robert C. Martin',                  27.50);

INSERT INTO inventory (book_id, available_quantity) SELECT id, 15 FROM books WHERE isbn = '9780132350884';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  8 FROM books WHERE isbn = '9780201633610';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  0 FROM books WHERE isbn = '9780134757599';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  1 FROM books WHERE isbn = '9780321125217';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 20 FROM books WHERE isbn = '9780135957059';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  0 FROM books WHERE isbn = '9780321146533';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 12 FROM books WHERE isbn = '9780134494166';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  6 FROM books WHERE isbn = '9781449373320';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  9 FROM books WHERE isbn = '9780596007126';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  5 FROM books WHERE isbn = '9780137081073';

-- 30 books in total, so the default page size of 20 gives two pages.

INSERT INTO books (isbn, title, author, price) VALUES
    ('9780131177055', 'Working Effectively with Legacy Code', 'Michael Feathers', 44.99),
    ('9780321127426', 'Patterns of Enterprise Application Architecture', 'Martin Fowler', 54.99),
    ('9780321213358', 'Refactoring to Patterns', 'Joshua Kerievsky', 47.50),
    ('9780321503626', 'Growing Object-Oriented Software, Guided by Tests', 'Steve Freeman & Nat Pryce', 41.99),
    ('9780134685991', 'Effective Java', 'Joshua Bloch', 45.00),
    ('9780321349606', 'Java Concurrency in Practice', 'Brian Goetz', 49.99),
    ('9781680502398', 'Release It!', 'Michael T. Nygard', 39.95),
    ('9781942788331', 'Accelerate', 'Nicole Forsgren, Jez Humble & Gene Kim', 29.99),
    ('9781942788294', 'The Phoenix Project', 'Gene Kim, Kevin Behr & George Spafford', 24.99),
    ('9780321601919', 'Continuous Delivery', 'Jez Humble & David Farley', 52.99),
    ('9781492034025', 'Building Microservices', 'Sam Newman', 49.99),
    ('9780321834577', 'Implementing Domain-Driven Design', 'Vaughn Vernon', 54.99),
    ('9780735619678', 'Code Complete', 'Steve McConnell', 49.99),
    ('9780201835953', 'The Mythical Man-Month', 'Frederick P. Brooks Jr.', 34.99),
    ('9780135781869', 'Clean Agile', 'Robert C. Martin', 31.99),
    ('9781732102200', 'A Philosophy of Software Design', 'John Ousterhout', 29.95),
    ('9780262510875', 'Structure and Interpretation of Computer Programs', 'Harold Abelson & Gerald Jay Sussman', 55.00),
    ('9780262046305', 'Introduction to Algorithms', 'Thomas H. Cormen et al.', 99.00),
    ('9781491929124', 'Site Reliability Engineering', 'Betsy Beyer et al.', 44.99),
    ('9780134434421', 'Domain-Driven Design Distilled', 'Vaughn Vernon', 27.99);

INSERT INTO inventory (book_id, available_quantity) SELECT id,  7 FROM books WHERE isbn = '9780131177055';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  4 FROM books WHERE isbn = '9780321127426';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  6 FROM books WHERE isbn = '9780321213358';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  5 FROM books WHERE isbn = '9780321503626';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 18 FROM books WHERE isbn = '9780134685991';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  3 FROM books WHERE isbn = '9780321349606';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  2 FROM books WHERE isbn = '9781680502398';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 14 FROM books WHERE isbn = '9781942788331';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 25 FROM books WHERE isbn = '9781942788294';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  8 FROM books WHERE isbn = '9780321601919';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 11 FROM books WHERE isbn = '9781492034025';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  6 FROM books WHERE isbn = '9780321834577';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  9 FROM books WHERE isbn = '9780735619678';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 12 FROM books WHERE isbn = '9780201835953';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 10 FROM books WHERE isbn = '9780135781869';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 16 FROM books WHERE isbn = '9781732102200';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  4 FROM books WHERE isbn = '9780262510875';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  0 FROM books WHERE isbn = '9780262046305';
INSERT INTO inventory (book_id, available_quantity) SELECT id,  7 FROM books WHERE isbn = '9781491929124';
INSERT INTO inventory (book_id, available_quantity) SELECT id, 13 FROM books WHERE isbn = '9780134434421';

-- Demo account: demo@example.com / demo1234 (BCrypt hash only; the password is never stored).
INSERT INTO users (email, password_hash, full_name, role) VALUES
    ('demo@example.com', '$2a$10$MW5dlTwZDoQedbRuS.EAF.hEZTFwaGAOATAeJUZ4bWEsiDqHzSlja', 'Demo User', 'USER');
