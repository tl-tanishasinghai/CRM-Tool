'use client'

// src/components/navbar/Navbar.js
import React, { useState } from "react";
import "./Navbar.scss";
import tlLogo from "@/assets/images/bharatpe-capital-logo-haorizontal.svg";
import carretDown from "@/assets/images/carretDown.svg";
import { useNavigate } from "@/utils/navigation-compat";

const baseUrl =
  process.env.NEXT_PUBLIC_TRILLION_ENDPOINT || "https://www.bharatpecapital.com";

const NAVIGATION_List = [
  {
    label: "Home",
    path: `${baseUrl}`,
  },
  {
    label: "About Us",
    path: `${baseUrl}/aboutUs`,
  },
  {
    label: "Products",
    subMenu: [

      {
        label: "Personal Loan",
        path: `${baseUrl}/product/personal-loan`,
      },
      {
        label: "Embedded Finance",
        path: `${baseUrl}/product/embedded-finance`,
      },
      {
        label: "Supply Chain Finance",
        path: `${baseUrl}/product/supply-chain-finance`,
      },
      {
        label: "Business Loan",
        path: `${baseUrl}/product/business-term-loan`,
      },
      {
        label: "Merchant Loan",
        path: `${baseUrl}/product/merchant-loan`,
      },
    ],
  },
  {
    label: "Partnerships",
    path: `${baseUrl}/partnerships`,
  },
  {
    label: "Policies & Codes",
    path: `${baseUrl}/policies-codes`,
  },
  {
    label: "Careers",
    path: `${baseUrl}/careers`,
  },
  {
    label: "Contact Us",
    path: `${baseUrl}/contactUs`,
  },
];

type NavItem = {
  label: string;
  path?: string;
  subMenu?: NavItem[];
};

type NavbarProps = {
  onLogout: () => void;
};

const Navbar = ({ onLogout }: NavbarProps) => {
  const [isHamburgerMenuOpen, setIsHamburgerMenuOpen] = useState(false);
  const navigate = useNavigate();

  const toggleHamburger = () => {
    setIsHamburgerMenuOpen(!isHamburgerMenuOpen);
  };

  const handleLogout = () => {
    onLogout();
    setIsHamburgerMenuOpen(false);
  };
  return (
    <nav className="navbar">
      <div className="navbar-container">
        <button
          className="hamburger"
          onClick={toggleHamburger}
          type="button"
          aria-label="Toggle navigation"
        >
          {isHamburgerMenuOpen ? "✕" : "☰"}
        </button>
        <div className="navbar-logo">
          <img src={tlLogo.src} alt="Logo" />
        </div>
        {/* <ul className="navbar-links">
        <li><a href="/">Home</a></li>
        <li><a href="/about">About Us</a></li>
        <li className="dropdown">
          <a href="/products" className="dropbtn">Products</a>
          <div className="dropdown-content">
            <a href="/product1">Product 1</a>
            <a href="/product2">Product 2</a>
            <a href="/product3">Product 3</a>
          </div>
        </li>
        <li><a href="/partnerships">Partnerships</a></li>
        <li><a href="/policies">Policies & Codes</a></li>
        <li><a href="/careers">Careers</a></li>
        <li><a href="/contact">Contact Us</a></li>
      </ul> */}
        <NavigationSection
          handleLogout={handleLogout}
          navigationList={NAVIGATION_List}
          isHamburgerMenuOpen={isHamburgerMenuOpen}
        />
        
      </div>
    </nav>
  );
};

const NestedNavMenu = ({ label, subMenu }: NavItem) => {

  const [hovering, setHovering] = useState(false);
  const handleMouseEnter = () => {
    setHovering(true);
  };
  const handleMouseLeave = () => {
    setHovering(false);
  };
  // const handleSubMenuClick = () => {
  //   setHovering(!hovering);
  // };

  return (
    <div
      className="dropdown-wrapper"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <button 
        className="sub-menu-container" 
        type="button"
        aria-expanded={hovering}
        aria-haspopup="menu"
      >
        {label} <img src={carretDown.src} alt="" aria-hidden="true" />
      </button>

      {hovering && (
        <div className="sub-menu" >
          {subMenu?.map((subNav, index) => (
            <a key={index} href={subNav?.path}>
              {subNav?.label}
            </a>
          ))}
        </div>
      )}
    </div>
  );
};

const renderNavItem = (nav: NavItem, index: number) => {
  const { label, subMenu, path } = nav;
  if (nav?.subMenu) {
    return (
      <li key={index} className="navbar-item">
        <NestedNavMenu label={label} subMenu={subMenu} />
      </li>
    );
  }
  return (
    <li key={index} className="navbar-item">
      <a href={path} target="_self">
        {label}
      </a>
    </li>
  );
};

type NavigationSectionProps = {
  navigationList: NavItem[];
  isHamburgerMenuOpen: boolean;
  handleLogout: () => void;
};

const NavigationSection = ({
  navigationList,
  isHamburgerMenuOpen,
  handleLogout,
}: NavigationSectionProps) => {
  return (
    <ul className={`navbar-links ${isHamburgerMenuOpen ? "open" : ""}`}>
      {/* {navigationList?.map(renderNavItem)} */}
      {navigationList?.map((item, index) => renderNavItem(item, index))}
      <li className="navbar-item">
        <button className="logout-link" onClick={handleLogout} type="button">
          Logout
        </button>
      </li>
    </ul>
  );
};

export default Navbar;
