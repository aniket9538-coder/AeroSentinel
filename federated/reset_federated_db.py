import psycopg2

conn = psycopg2.connect(
    host="localhost",
    port=5432,
    dbname="aerosentinel",
    user="aerosentinel_user",
    password="change_this_in_production",
)
cur = conn.cursor()
cur.execute("DELETE FROM model_updates;")
cur.execute("DELETE FROM federated_node_updates;")
cur.execute("DELETE FROM federated_rounds;")
cur.execute("DELETE FROM federated_global_models WHERE version != 'global-v1';")
cur.execute("UPDATE federated_global_models SET is_active = TRUE WHERE version = 'global-v1';")
conn.commit()

cur.execute("SELECT count(*) FROM federated_rounds;")
print("Rounds count:", cur.fetchone()[0])
cur.execute("SELECT version, is_active FROM federated_global_models;")
print("Models:", cur.fetchall())
conn.close()
