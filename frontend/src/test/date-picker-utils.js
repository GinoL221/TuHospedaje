const MONTH_MAP = {
	January: "enero",
	February: "febrero",
	March: "marzo",
	April: "abril",
	May: "mayo",
	June: "junio",
	July: "julio",
	August: "agosto",
	September: "septiembre",
	October: "octubre",
	November: "noviembre",
	December: "diciembre",
};

function normalizeLabelPart(labelPart) {
	// e.g. "July 15th, 2026" -> ["15 de julio de 2026", "July 15th, 2026"]
	const match = labelPart.match(
		/^([A-Za-z]+)\s+(\d{1,2})(?:st|nd|rd|th)?,?\s*(\d{4})?$/,
	);
	if (match) {
		const [, monthEn, day, year] = match;
		const monthEs = MONTH_MAP[monthEn];
		if (monthEs) {
			const esFormat = year
				? `${day} de ${monthEs} de ${year}`
				: `${day} de ${monthEs}`;
			return [esFormat, labelPart];
		}
	}
	return [labelPart];
}

export function getDateCellByLabelPart(labelPart) {
	const candidates = normalizeLabelPart(labelPart);
	const cell = Array.from(document.querySelectorAll('[role="gridcell"]')).find(
		(day) => {
			const aria = day.getAttribute("aria-label");
			return aria && candidates.some((c) => aria.includes(c));
		},
	);

	if (!cell) {
		throw new Error(`Date cell not found for label part: ${labelPart}`);
	}

	return cell;
}

export async function selectDateByLabelPart(user, input, labelPart) {
	await user.click(input);
	const cell = getDateCellByLabelPart(labelPart);
	await user.click(cell);
	return cell;
}
