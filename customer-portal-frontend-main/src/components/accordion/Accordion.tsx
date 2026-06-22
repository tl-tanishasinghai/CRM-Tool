'use client'

import React, { useState } from "react";
import "./Accordion.scss";
import carretDown from "@/assets/images/carretDown.svg";
import { useNavigate } from "@/utils/navigation-compat";

type AccordionItemData = {
  title: string;
  content: React.ReactNode;
};

type AccordionProps = {
  accordionList: AccordionItemData[];
  showAllFaqCta?: boolean;
};

const Accordion = ({ accordionList, showAllFaqCta = false }: AccordionProps) => {
  const navigate = useNavigate();

  return (
    <div>
      <div className="widget-header">
        <div className="widget-heading">FAQs</div>
        {showAllFaqCta && (
          <button
            className="widget-actionable transaction-button"
            onClick={() => navigate("/faqs")}
            type="button"
          >
            View All FAQs
          </button>
        )}
      </div>
      {accordionList.map((item, index) => (
        <AccordionItem key={index} title={item.title} content={item.content} />
      ))}
    </div>
  );
};

type AccordionItemProps = {
  title: string;
  content: React.ReactNode;
};

const AccordionItem = ({ title, content }: AccordionItemProps) => {
  const [isOpen, setIsOpen] = useState(false);

  const toggleAccordion = () => {
    setIsOpen(!isOpen);
  };

  return (
    <div>
      <div className="accordion-item" onClick={toggleAccordion}>
        <p>{title}</p>
        <img
          className={`accordion-icon ${isOpen ? "open-accordion" : ""}`}
          src={carretDown.src}
          alt={isOpen ? "Collapse" : "Expand"}
        />
      </div>
      {isOpen && <div className="accordion-content">{content}</div>}
    </div>
  );
};

export default Accordion;
