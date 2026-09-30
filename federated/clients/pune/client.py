"""
AeroSentinel - Pune Municipal Node Client
File: federated/clients/pune/client.py

Coordinates local training rounds and packages parameter updates for the Pune municipal node.
"""

from typing import Optional, Any
from federated.clients.base_client import BaseFederatedClient
from federated.clients.pune.trainer import PuneTrainer
from federated.clients.pune.local_data import PUNE_NODE_ID, PUNE_CITY_ID


class PuneClient(BaseFederatedClient):
    """
    Pune municipal node federated client.
    """

    def __init__(self, random_state: int = 42, network_client: Optional[Any] = None):
        trainer = PuneTrainer(random_state=random_state)
        super().__init__(
            node_id=PUNE_NODE_ID,
            city_id=PUNE_CITY_ID,
            trainer=trainer,
            network_client=network_client,
        )
