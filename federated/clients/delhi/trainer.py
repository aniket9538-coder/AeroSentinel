"""
AeroSentinel - Delhi Regional Node Trainer
File: federated/clients/delhi/trainer.py

Trains local hotspot classification model on Delhi municipal regional data.
"""

from federated.clients.base_client import BaseCityTrainer
from federated.clients.delhi.local_data import (
    DelhiLocalDataLoader,
    DELHI_NODE_ID,
    DELHI_CITY_ID,
)


class DelhiTrainer(BaseCityTrainer):
    """
    Delhi local model trainer.
    """

    def __init__(self, random_state: int = 202):
        data_loader = DelhiLocalDataLoader(random_seed=random_state)
        super().__init__(
            node_id=DELHI_NODE_ID,
            city_id=DELHI_CITY_ID,
            data_loader=data_loader,
            random_state=random_state,
        )
