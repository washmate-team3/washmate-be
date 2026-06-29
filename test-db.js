const { Client } = require('pg');

async function check() {
  const client = new Client({
    connectionString: process.env.DB_URL
  });
  await client.connect();
  const res = await client.query('SELECT p.id, p.status as payment_status, p.amount, b.status as booking_status, b.final_amount, t.status as tx_status, t.raw_response FROM payments p JOIN bookings b ON p.booking_id = b.id LEFT JOIN payment_transactions t ON t.payment_id = p.id ORDER BY p.id DESC LIMIT 5');
  console.log(JSON.stringify(res.rows, null, 2));
  await client.end();
}

require('dotenv').config();
check().catch(console.error);
