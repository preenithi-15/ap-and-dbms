import unittest
from service import compare, similarity

class SimilarityTests(unittest.TestCase):
    def test_identical_feature_sequences_score_full(self):
        self.assertEqual(similarity(["login", "shell"], ["login", "shell"]), 100.0)

    def test_sequence_order_changes_alignment_score(self):
        from service import sequence_similarity
        self.assertLess(sequence_similarity(["document", "shell", "script"], ["document", "script", "shell"]), 100.0)

    def test_comparison_exposes_all_five_dimensions_and_caveat(self):
        events = [{"id": 1, "relativeSeconds": 0, "type": "PROCESS", "sequence": "doc>shell", "target": "host", "behavior": "spawn", "progression": "execution", "certainty": "OBSERVED", "evidenceRef": "EV-A"}, {"id": 2, "relativeSeconds": 10, "type": "NETWORK", "sequence": "shell>net", "target": "network", "behavior": "tls", "progression": "egress", "certainty": "INFERRED", "evidenceRef": "EV-B"}]
        output = compare({"featuresA": {"caseId": 1, "events": events, "missingCount": 1, "coverageNote": "synthetic"}, "featuresB": {"caseId": 2, "events": events, "missingCount": 0, "coverageNote": "synthetic"}})
        self.assertEqual([d["key"] for d in output["dimensions"]], ["sequence", "timing", "target", "behavior", "progression"])
        self.assertEqual(output["overall"], 100.0)
        self.assertIn("not attribution", output["caveat"])
        self.assertTrue(output["dimensions"][0]["matchedEvents"])
        self.assertTrue(output["dimensions"][1]["matchedEvents"])
        self.assertIn("Case A has 1 telemetry gap", output["dimensions"][0]["missing"][0])

if __name__ == "__main__":
    unittest.main()
