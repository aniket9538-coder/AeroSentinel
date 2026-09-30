"""
AeroSentinel - Pune Municipal Node Trainer
File: federated/clients/pune/trainer.py

Trains local hotspot classification model on Pune municipal data.
"""

from federated.clients.base_client import BaseCityTrainer
from federated.clients.pune.local_data import (
    PuneLocalDataLoader,
    PUNE_NODE_ID,
    PUNE_CITY_ID,
)


class PuneTrainer(BaseCityTrainer):
    """
    Pune local model trainer.
    """

    def __init__(self, random_state: int = 42):
        data_loader = PuneLocalDataLoader(random_seed=random_state)
        super().__init__(
            node_id=PUNE_NODE_ID,
            city_id=PUNE_CITY_ID,
            data_loader=data_loader,
            random_state=random_state,
        )
