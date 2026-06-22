'use client'

import React from "react";
import "./ToolTip.scss";

type ToolTipProps = {
  text: string;
  children: React.ReactNode;
};

const ToolTip = ({ text, children }: ToolTipProps) => {
  return (
    <div className="tooltip-wrapper-v1">
      {children}
      <div className="tooltip-box">{text}</div>
    </div>
  );
};

export default ToolTip;
