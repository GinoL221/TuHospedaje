export function hasReservationNotes(notes) {
  return typeof notes === "string" && notes.trim().length > 0;
}

export function reservationCreatedAtLabel({ createdAt, createdAtDerived }) {
  if (!createdAt) return null;

  const [date, time = ""] = createdAt.split("T");
  const formattedDate = date.split("-").reverse().join("/");
  const formattedTime = time.slice(0, 5);
  const label = createdAtDerived ? "Fecha estimada" : "Fecha de creación";

  return `${label}: ${formattedDate} ${formattedTime}`.trim();
}
