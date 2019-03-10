/*
 * Copyright (c) 2018-2019 Valentin D'Emmanuele, Gilles Mertens, Dylan Fraisse, Hugo Chemarin, Nicolas Gervasi
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package projetarm_v2.simulator.ui.javafx.ramview;

public enum ShowType {
	WORD,
	HALFWORD,
	BYTE;
	
	public int toOffset() {
		switch (this) {
		case WORD:
			return 4;
		case HALFWORD:
			return 2;
		default:
		case BYTE:
			return 1;
		}
	}
}
