CREATE TABLE public_holiday (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    holiday_date DATE         NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL
);