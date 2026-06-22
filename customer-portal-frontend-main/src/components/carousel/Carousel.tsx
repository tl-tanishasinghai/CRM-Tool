'use client'

import React, { useEffect, useRef, useState } from "react";
import "./Carousel.scss";
import rightArrowBlue from "@/assets/images/Arrow-Right-lightBlue.svg";

type CarouselProps = {
  cards: React.ReactNode[];
};

const Carousel = ({ cards }: CarouselProps) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [isLeftArrowVisible, setIsLeftArrowVisible] = useState(false);
  const [isRightArrowVisible, setIsRightArrowVisible] = useState(true);

  const handleScroll = () => {
    const container = containerRef.current;
    if (!container) return;

    setIsLeftArrowVisible(container.scrollLeft > 0);
    setIsRightArrowVisible(
      container.scrollLeft + container.offsetWidth < container.scrollWidth
    );
  };

  const scroll = (direction: "left" | "right") => {
    const container = containerRef.current;
    if (!container) return;

    const scrollAmount = container.offsetWidth;
    const newScrollPosition =
      direction === "left"
        ? container.scrollLeft - scrollAmount
        : container.scrollLeft + scrollAmount;

    container.scrollTo({ left: newScrollPosition, behavior: "smooth" });
  };

  useEffect(() => {
    handleScroll(); // Initial check
  }, [cards]);

  return (
    <div className="tabs-container">
      {isLeftArrowVisible && (
        <button
          className="arrow left-arrow"
          onClick={() => scroll("left")}
          type="button"
        >
          <img
            className="left-nav-arrow"
            src={rightArrowBlue.src}
            alt="Scroll left"
          />
        </button>
      )}

      <div className="tabs-wrapper" ref={containerRef} onScroll={handleScroll}>
        {cards}
      </div>

      {isRightArrowVisible && cards.length > 1 && (
        <button
          className="arrow right-arrow"
          onClick={() => scroll("right")}
          type="button"
        >
          <img src={rightArrowBlue.src} alt="Scroll right" />
        </button>
      )}
    </div>
  );
};

export default Carousel;
