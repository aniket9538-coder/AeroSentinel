"""
AeroSentinel - Delhi Regional Node Client
File: federated/clients/delhi/client.py

Coordinates local training rounds and packages parameter updates for the Delhi regional node.
"""

from typing import Optional, Any
from federated.clients.base_client import BaseFederatedClient
from federated.clients.delhi.trainer import DelhiTrainer
from federated.clients.delhi.local_data import DELHI_NODE_ID, DELHI_CITY_ID


class DelhiClient(BaseFederatedClient):
    """
    Delhi regional node federated client.
    """

    def __init__(self, random_state: int = 202, network_client: Optional[Any] = None):
        trainer = DelhiTrainer(random_state=random_state)
        super().__init__(
            node_id=DELHI_NODE_ID,
            city_id=DELHI_CITY_ID,
            trainer=trainer,
            network_client=network_client,
        )
