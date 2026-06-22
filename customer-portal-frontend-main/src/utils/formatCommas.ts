function formatWithCommas(value: string | number, prefix = "") {
  const num = Number(value);
  if (!Number.isFinite(num)) return "-";
  if (num === 0) return "-";
  return `${prefix}${num.toLocaleString()}`;
}

export default formatWithCommas;