/*
 * The Exomiser - A tool to annotate and prioritize variants
 *
 * Copyright (C) 2012 - 2015  Charite Universitätsmedizin Berlin and Genome Research Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.charite.compbio.exomiser.core.model;

import de.charite.compbio.jannovar.annotation.VariantEffect;

/**
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class RegulatoryFeature implements ChromosomalRegion {

    enum FeatureType{ENHANCER, PROMOTER};

    private final int chromosome;
    private final int start;
    private final int end;
    private final VariantEffect featureType;

    public RegulatoryFeature(int chromosome, int start, int end, VariantEffect featureType) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.featureType = featureType;
    }

    public int getChromosome() {
        return chromosome;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public VariantEffect getFeatureType() {
        return featureType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegulatoryFeature that = (RegulatoryFeature) o;

        if (chromosome != that.chromosome) return false;
        if (start != that.start) return false;
        if (end != that.end) return false;
        return featureType == that.featureType;

    }

    @Override
    public int hashCode() {
        int result = chromosome;
        result = 31 * result + start;
        result = 31 * result + end;
        result = 31 * result + featureType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RegulatoryFeature{" +
                "chromosome=" + chromosome +
                ", start=" + start +
                ", end=" + end +
                ", featureType=" + featureType +
                '}';
    }
}
