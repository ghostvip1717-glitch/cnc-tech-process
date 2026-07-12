ROMAN_ORDERS = ("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")


def setup_order_label(order: int) -> str:
    if 0 <= order < len(ROMAN_ORDERS):
        return ROMAN_ORDERS[order]
    return str(order + 1)
