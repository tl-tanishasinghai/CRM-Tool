'use client'

import React, { useEffect, useRef, useState } from "react";
import "./DOBInput.scss";

type DOBInputProps = {
  error?: string;
  onChange?: (value: string) => void;
};

const DOBInput = ({ error, onChange }: DOBInputProps) => {
  const [day, setDay] = useState("");
  const [month, setMonth] = useState("");
  const [year, setYear] = useState("");

  const monthRef = useRef<HTMLInputElement | null>(null);
  const yearRef = useRef<HTMLInputElement | null>(null);
  const dayRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (day.length === 2 && month.length === 2 && year.length === 4) {
      onChange?.(`${day}-${month}-${year}`);
    } else {
      onChange?.('');
    }
  }, [day, month, year, onChange]);

  const handleDayChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 2) value = value.slice(0, 2);
    setDay(value);
    if (value.length === 2) {
      monthRef.current?.focus();
    }
  };

  const handleMonthChange = (e: React.ChangeEvent<HTMLInputElement>) => {

    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 2) value = value.slice(0, 2);

    setMonth(value);
    if (value.length === 2) {
      yearRef.current?.focus();
    }
  };

  const handleYearChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 4) value = value.slice(0, 4);
    setYear(value);
  };

  const handleYearKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && year.length === 0) {
      monthRef.current?.focus();
    }
  };

  const handleMonthKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace" && month.length === 0) {
      dayRef.current?.focus();
    }
  };


  return (
    <div className="dob-input">
      
      <div className="dob-input__fields" role="group" aria-label="Date of birth">
        <input
          type="text"
          className="dob-input__field"
          value={day}
          onChange={handleDayChange}
          placeholder="DD"
          maxLength={2}
          ref={dayRef}
          aria-label="Day"
        />
        <span className="dob-input__separator">/</span>
        <input
          type="text"
          className="dob-input__field"
          value={month}
          onChange={handleMonthChange}
          onKeyDown={handleMonthKeyDown}
          placeholder="MM"
          maxLength={2}
          ref={monthRef}
          aria-label="Month"
        />
        <span className="dob-input__separator" aria-hidden="true">/</span>
        <input
          type="text"
          className="dob-input__field year-input"
          value={year}
          onChange={handleYearChange}
          onKeyDown={handleYearKeyDown}
          placeholder="YYYY"
          maxLength={4}
          ref={yearRef}
          aria-label="Year"
        />
      </div>
      {error && <div className="dob-input__error">{error}</div>}
    </div>
  );
};

export default DOBInput;
