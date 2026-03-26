export function formatDateTime(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}.${m}.${day} ${hh}:${mm}`;
}

export function formatDate(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}.${m}.${day}`;
}

export function toInputDate(value: string): string {
  return formatDate(value).replaceAll(".", "-");
}

export function calcNights(admitDate: string, dischargeDate: string): number {
  const a = new Date(admitDate);
  const d = new Date(dischargeDate);
  const ms = d.getTime() - a.getTime();
  const nights = Math.floor(ms / (1000 * 60 * 60 * 24));
  return Number.isFinite(nights) && nights > 0 ? nights : 0;
}

export function periodLabel(admitDate: string, dischargeDate: string): string {
  return `${formatDate(admitDate)} ~ ${formatDate(dischargeDate)}`;
}
