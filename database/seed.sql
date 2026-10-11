USE threattrace;
INSERT INTO investigation_cases(case_id,case_name,classification,is_synthetic) VALUES
(1,'Glass Finch','Credential access / lateral movement',TRUE),(2,'Copper Harbor','Script execution / outbound activity',TRUE)
ON DUPLICATE KEY UPDATE case_name=VALUES(case_name);
INSERT INTO digital_twin_nodes(node_id,case_id,label,node_type,criticality,certainty) VALUES
(1,1,'WS-ALPHA','WORKSTATION',2,'OBSERVED'),(2,1,'ID-OPS','ACCOUNT',3,'OBSERVED'),(3,1,'FILE-CORE','FILE_SERVER',5,'CONFIGURED'),(4,1,'NET-EGRESS','NETWORK',4,'UNKNOWN'),
(5,2,'WS-BRAVO','WORKSTATION',2,'OBSERVED'),(6,2,'ID-FIN','ACCOUNT',3,'OBSERVED'),(7,2,'FILE-VAULT','FILE_SERVER',5,'CONFIGURED'),(8,2,'NET-EGRESS','NETWORK',4,'UNKNOWN')
ON DUPLICATE KEY UPDATE label=VALUES(label);
INSERT INTO digital_twin_edges(edge_id,case_id,from_node_id,to_node_id,edge_type,certainty,description) VALUES
(1,1,1,2,'AUTHENTICATES_AS','OBSERVED','Observed interactive session'),(2,1,2,3,'CAN_ACCESS','CONFIGURED','Configured file service dependency'),(3,1,3,4,'MAY_CONNECT','UNKNOWN','Outbound route telemetry unavailable'),
(4,2,5,6,'AUTHENTICATES_AS','OBSERVED','Observed service session'),(5,2,6,7,'CAN_ACCESS','CONFIGURED','Configured share access'),(6,2,7,8,'MAY_CONNECT','INFERRED','Possible outbound path from event correlation')
ON DUPLICATE KEY UPDATE description=VALUES(description);
INSERT INTO event_logs(event_id,case_id,occurred_at,event_type,source_label,description,evidence_ref,certainty,host_label,parent_event_id,feature_sequence,feature_target,feature_behavior,feature_progression) VALUES
(101,1,'2026-04-14 09:00:00.000','IDENTITY','Auth gateway','Unusual sign-in accepted for an operations account','EV-GLASS-001','OBSERVED','WS-ALPHA',NULL,'login','workstation','credential_use','initial_access'),
(102,1,'2026-04-14 09:00:07.000','PROCESS','Endpoint sensor','Document viewer spawned a command shell','EV-GLASS-002','OBSERVED','WS-ALPHA',101,'document>shell','workstation','process_spawn','execution'),
(103,1,'2026-04-14 09:00:09.000','PROCESS','Endpoint sensor','Shell launched a script interpreter','EV-GLASS-003','OBSERVED','WS-ALPHA',102,'shell>script','workstation','script_execution','execution'),
(104,1,'2026-04-14 09:43:00.000','NETWORK','DNS sensor','Rare external domain lookup observed after an extended quiet interval','EV-GLASS-004','OBSERVED','WS-ALPHA',103,'script>dns','network','dns_lookup','command_control'),
(105,1,'2026-04-14 09:44:12.000','SYSTEM','Telemetry monitor','File server audit coverage unavailable for 11 minutes','EV-GLASS-GAP','MISSING_TELEMETRY','FILE-CORE',NULL,'unknown','file_server','telemetry_gap','unknown'),
(106,1,'2026-04-14 10:01:00.000','FILE','File audit','Sensitive directory listing recorded; access outcome is incomplete','EV-GLASS-006','INFERRED','FILE-CORE',104,'dns>share','file_server','directory_access','lateral_movement'),
(201,2,'2026-05-02 12:10:00.000','IDENTITY','Auth gateway','Service account sign-in from a new workstation','EV-COPPER-001','OBSERVED','WS-BRAVO',NULL,'login','workstation','credential_use','initial_access'),
(202,2,'2026-05-02 12:10:08.000','PROCESS','Endpoint sensor','Document viewer spawned a command shell','EV-COPPER-002','OBSERVED','WS-BRAVO',201,'document>shell','workstation','process_spawn','execution'),
(203,2,'2026-05-02 12:10:11.000','PROCESS','Endpoint sensor','Shell launched a script interpreter','EV-COPPER-003','OBSERVED','WS-BRAVO',202,'shell>script','workstation','script_execution','execution'),
(204,2,'2026-05-02 12:10:44.000','NETWORK','Proxy sensor','Outbound TLS connection followed script execution','EV-COPPER-004','OBSERVED','WS-BRAVO',203,'script>network','network','outbound_tls','command_control'),
(205,2,'2026-05-02 12:18:00.000','SYSTEM','Telemetry monitor','Identity audit stream has a short collection gap','EV-COPPER-GAP','MISSING_TELEMETRY','ID-FIN',NULL,'unknown','account','telemetry_gap','unknown'),
(206,2,'2026-05-02 12:33:00.000','FILE','File audit','Share metadata read from a sensitive server','EV-COPPER-006','OBSERVED','FILE-VAULT',204,'network>share','file_server','directory_access','lateral_movement')
ON DUPLICATE KEY UPDATE description=VALUES(description);
INSERT INTO hypotheses(hypothesis_id,case_id,title,status,confidence,rationale,missing_telemetry) VALUES
(1,1,'Credential-led script activity may be related to later file access','OPEN',0.610,'Observed login and process ancestry precede file activity, but the long gap has limited telemetry.','File server audit gap; no confirmed transfer record.'),
(2,2,'Script execution may have led to outbound communication','SUPPORTED',0.720,'Observed parent-child process chain and outbound TLS event are temporally close.','Network payload and identity stream are unavailable.')
ON DUPLICATE KEY UPDATE title=VALUES(title);
INSERT INTO hypothesis_evidence(hypothesis_id,event_id,relation,note) VALUES
(1,101,'SUPPORTS','Unusual accepted sign-in starts the observed sequence'),(1,102,'SUPPORTS','Process lineage links document viewer to shell'),(1,106,'SUPPORTS','File activity appears later, outcome incomplete'),(1,105,'CONTRADICTS','Telemetry gap limits causal interpretation'),
(2,202,'SUPPORTS','Observed shell parent process'),(2,203,'SUPPORTS','Observed script child process'),(2,204,'SUPPORTS','Outbound connection observed soon after execution'),(2,205,'CONTRADICTS','Identity telemetry gap permits alternative explanations')
ON DUPLICATE KEY UPDATE note=VALUES(note);
INSERT INTO mitigation_strategies(strategy_id,label,risk_reduction,business_disruption,evidence_impact,estimated_minutes) VALUES
(1,'Isolate affected workstation',72,34,18,8),(2,'Disable suspected account',58,48,8,12),(3,'Block observed outbound destination',36,12,4,5)
ON DUPLICATE KEY UPDATE label=VALUES(label);
