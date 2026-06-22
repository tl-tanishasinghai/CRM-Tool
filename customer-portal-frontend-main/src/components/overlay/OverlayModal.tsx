'use client'

// src/components/OverlayModal.js
import React from "react";
import "./OverlayModal.css";

type OverlayModalProps = {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
};

const OverlayModal = ({ isOpen, onClose, children }: OverlayModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="overlay-backdrop">
      <div className="overlay-modal">
        <button className="overlay-close-btn" onClick={onClose} type="button">
          &times;
        </button>
        <div className="overlay-content">{children}</div>
      </div>
    </div>
  );
};

export default OverlayModal;
