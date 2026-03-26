export function formatCurrency(amount: number): string {
  return `${amount.toLocaleString("ko-KR")}원`;
}
