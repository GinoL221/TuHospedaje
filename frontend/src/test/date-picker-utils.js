export function getDateCellByLabelPart(labelPart) {
	const cell = Array.from(document.querySelectorAll('[role="gridcell"]')).find(
		(day) => day.getAttribute("aria-label")?.includes(labelPart),
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
