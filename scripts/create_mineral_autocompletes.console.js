(() => {
  /* Paste this block of code into your console on
  ** https://starcitizen.tools/Ore_quality
  ** To generate a JSON data block of autocomplete values
  */

  const results = [];

  document.querySelectorAll('table tbody tr').forEach(row => {
    const cells = [...row.querySelectorAll('td, th')].map(c => c.textContent.trim());
    if (cells.length < 2) return;

    // Normalize item name (e.g., "Pressurized Ice" -> "pressurized_ice")
    const item = cells[0].toLowerCase().replace(/\s+/g, '_');

    // Skip header rows
    if (item === 'ore' || item === 'mineral') return;

    // Pair item name with each valid numeric quality in the row
    cells.slice(1).forEach(val => {
      if (/^\d+$/.test(val)) {
        results.push(`${item}_${val}`);
      }
    });
  });

  console.log("INSERT INTO loot_autocomplete VALUES (\"" + results.join("\"),\n(\"") + "\");");
  return results;
})();
