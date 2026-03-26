export function maskName(name: string): string {
  if (!name) return "";
  if (name.length <= 2) return `${name[0]}*`;
  return `${name[0]}*${name[name.length - 1]}`;
}

export function maskReservationName(name: string): string {
  if (!name) return "";
  if (name.length === 1) return "*";
  return `${name[0]}**`;
}

export function maskPhone(phone: string): string {
  const m = phone.replace(/[^0-9]/g, "");
  if (m.length < 8) return phone;
  if (m.length === 11) return `${m.slice(0, 3)}-${m.slice(3, 7)}-****`;
  return `${m.slice(0, 3)}-${m.slice(3, 6)}-****`;
}

export function formatRrnMasked(front: string, rrnBack: string): string {
  const first = rrnBack?.[0] ?? "*";
  return `${front}-${first}******`;
}

export function formatRrnFull(front: string, rrnBack: string): string {
  return `${front}-${rrnBack}`;
}
