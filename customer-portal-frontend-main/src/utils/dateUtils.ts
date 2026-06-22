export function isValidDate(dateStr: string) {
  const [dd, mm, yyyy] = dateStr.split("-").map(Number);

  // Basic range checks
  if (!dd || !mm || !yyyy || dd < 1 || mm < 1 || mm > 12 || yyyy < 1000 || yyyy > 9999) {
    return false;
  }

  // Create a Date object (months are 0-indexed in JS)
  const date = new Date(yyyy, mm - 1, dd);

  // Check if the date components match (to catch invalid dates like 31-02-2023)
  return (
    date.getFullYear() === yyyy &&
    date.getMonth() === mm - 1 &&
    date.getDate() === dd
  );
}

export function reverseDate(dateStr: string) {
  return dateStr.split("-").reverse().join("-");
}
  