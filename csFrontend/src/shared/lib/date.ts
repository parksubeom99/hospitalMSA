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

// [ADDED] 오늘 날짜를 YYYY-MM-DD 형식으로 반환 (input[type=date] 기본값용)
export function todayDate(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// [ADDED] N일 후 날짜를 YYYY-MM-DD 형식으로 반환 (퇴원일 기본값용)
export function daysAfterToday(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// [ADDED] 현재 시각을 datetime-local input 기본값 형식(YYYY-MM-DDTHH:mm)으로 반환
// 30분 단위 올림 → 예약 기본 시각으로 사용
export function nowDateTimeRounded(): string {
  const d = new Date();
  d.setMinutes(d.getMinutes() >= 30 ? 60 : 30, 0, 0);
  const y = d.getFullYear();
  const mo = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${y}-${mo}-${day}T${hh}:${mm}`;
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
