'use client'

import React from "react";
import Accordion from "@/components/accordion/Accordion";
import { FQAS } from "@/components/accordion/Constants";
import "./Faqs.scss";

const Faqs = () => {
  return (
    <div className="home-container">
      <Accordion accordionList={FQAS} />
    </div>
  );
};

export default Faqs;
