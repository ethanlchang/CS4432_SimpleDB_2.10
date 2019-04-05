//Examples.sql
//SQL version of Examples.java that can be run from command line

//clear tables
DELETE FROM Person;
DELETE FROM Account;

//create tables

CREATE TABLE Person(
    SSN INT,
    Name VARCHAR(50),
    AccountNumber INT
);
CREATE TABLE Account(
    AccountNumber INT,
    Balance FLOAT,
    OwnerSSN INT,
    BankBranch VARCHAR(25),

);

//populate tables
INSERT INTO Person (SSN, Name, AccountNumber)
    VALUES (1234567890 , 'John Smith', 1);
INSERT INTO Person (SSN, Name, AccountNumber)
    VALUES (0987654321 , 'Joe Shmoe', 2);
INSERT INTO Person (SSN, Name, AccountNumber)
    VALUES (2468013579 , 'Amy Sue', 3);
INSERT INTO Person (SSN, Name, AccountNumber)
    VALUES (1029384756 , 'Bob Lee', 4);
INSERT INTO Person (SSN, Name, AccountNumber)
    VALUES (6758493021 , 'Max Fax', 5);
INSERT INTO Account (AccountNumber, Balance, OwnerSSN, BankBranch)
    VALUES (1, 47, 1234567890, 'A');
INSERT INTO Account (AccountNumber, Balance, OwnerSSN, BankBranch)
    VALUES (2, 22225, 0987654321, 'B');
INSERT INTO Account (AccountNumber, Balance, OwnerSSN, BankBranch)
    VALUES (3, 39584, 2468013579, 'A');
INSERT INTO Account (AccountNumber, Balance, OwnerSSN, BankBranch)
    VALUES (4, 9583, 1029384756, 'C');
INSERT INTO Account (AccountNumber, Balance, OwnerSSN, BankBranch)
    VALUES (5, 64357, 6758493021, 'C');


//SQL queries

SELECT Name
FROM Person P
WHERE P.Name = 'John Smith';

SELECT AccountNumber, Balance, BankBranch
FROM Account A
WHERE A.BankBranch = 'A'
AND A.Balance = 47;

SELECT AccountNumber, Balance, BankBranch
FROM Account A
WHERE A.BankBranch = 'C';
