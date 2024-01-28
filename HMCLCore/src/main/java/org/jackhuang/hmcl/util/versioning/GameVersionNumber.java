package org.jackhuang.hmcl.util.versioning;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GameVersionNumber implements Comparable<GameVersionNumber> {

    public static GameVersionNumber asGameVersion(String version) {
        return null; // TODO
    }

    final String value;

    GameVersionNumber(String value) {
        this.value = value;
    }

    enum Type {
        PRE_CLASSIC, CLASSIC, INFDEV, ALPHA, BETA, NEW
    }

    abstract Type getType();

    abstract int compareToImpl(@NotNull GameVersionNumber other);

    public int compareTo(@NotNull String other) {
        return this.compareTo(asGameVersion(other));
    }

    @Override
    public int compareTo(@NotNull GameVersionNumber other) {
        if (this.getType() != other.getType()) {
            return this.getType().compareTo(other.getType());
        }

        return compareToImpl(other);
    }

    @Override
    public String toString() {
        return value;
    }

    static final class PreClassic extends GameVersionNumber {

        static PreClassic parse(String value) {
            int version;
            try {
                version = Integer.parseInt(value.substring("rd-".length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
            return new PreClassic(value, version);
        }

        private final int version;

        PreClassic(String value, int version) {
            super(value);
            this.version = version;
        }

        @Override
        Type getType() {
            return Type.PRE_CLASSIC;
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            return Integer.compare(this.version, ((PreClassic) other).version);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreClassic other = (PreClassic) o;
            return version == other.version;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(version);
        }
    }

    static final class Old extends GameVersionNumber {

        private static final Pattern PATTERN = Pattern.compile("[abc](?<major>[0-9]+)\\.(?<minor>[0-9]+)(\\.(?<patch>[0-9]+))?([^0-9]*(?<additional>[0-9]+).*)");

        static Old parse(String value) {
            Matcher matcher = PATTERN.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(value);
            }

            Type type;
            switch (value.charAt(0)) {
                case 'a':
                    type = Type.ALPHA;
                    break;
                case 'b':
                    type = Type.BETA;
                    break;
                case 'c':
                    type = Type.CLASSIC;
                    break;
                default:
                    throw new AssertionError(value);
            }

            int major = Integer.parseInt(matcher.group("major"));
            int minor = Integer.parseInt(matcher.group("minor"));

            String patchString = matcher.group("patch");
            int patch = patchString != null ? Integer.parseInt(patchString) : 0;

            String additionalString = matcher.group("additional");
            int additional = additionalString != null ? Integer.parseInt(additionalString) : 0;

            return new Old(value, type, major, minor, patch, additional);
        }

        private final Type type;
        private final int major;
        private final int minor;
        private final int patch;
        private final int additional;

        private Old(String value, Type type, int major, int minor, int patch, int additional) {
            super(value);
            this.type = type;
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.additional = additional;
        }

        @Override
        Type getType() {
            return type;
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            Old that = (Old) other;
            int c = Integer.compare(this.major, that.major);
            if (c != 0) {
                return c;
            }

            c = Integer.compare(this.minor, that.minor);
            if (c != 0) {
                return c;
            }

            c = Integer.compare(this.patch, that.patch);
            if (c != 0) {
                return c;
            }

            return Integer.compare(this.additional, that.additional);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Old other = (Old) o;
            return major == other.major && minor == other.minor && patch == other.patch && additional == other.additional && type == other.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, major, minor, patch, additional);
        }
    }

    static final class Infdev extends GameVersionNumber {

        static Infdev parse(String value) {
            String version = value.substring("inf-".length());
            int major;
            int patch;

            try {
                major = Integer.parseInt(version);
                patch = 0;
            } catch (NumberFormatException e) {
                int idx = version.indexOf('-');
                if (idx >= 0) {
                    try {
                        major = Integer.parseInt(version.substring(0, idx));
                        patch = Integer.parseInt(version.substring(idx + 1));
                    } catch (NumberFormatException ignore) {
                        throw new IllegalArgumentException(value);
                    }
                } else {
                    throw new IllegalArgumentException(value);
                }
            }

            return new Infdev(value, major, patch);
        }

        private final int major;
        private final int patch;

        Infdev(String value, int major, int patch) {
            super(value);
            this.major = major;
            this.patch = patch;
        }

        @Override
        Type getType() {
            return Type.INFDEV;
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            Infdev that = (Infdev) other;
            int c = Integer.compare(this.major, that.major);
            return c != 0 ? c : Integer.compare(this.patch, that.patch);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Infdev other = (Infdev) o;
            return major == other.major && patch == other.patch;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, patch);
        }
    }

    static final class Release extends GameVersionNumber {

        private static final Pattern PATTERN = Pattern.compile("(?<major>[0-9]+)\\.(?<minor>[0-9]+)(\\.(?<patch>[0-9]+))?((?<eaType>(-[a-zA-Z]+| Pre-Release ))(?<eaVersion>[0-9]+))?");

        private static final int GA = Integer.MAX_VALUE;

        private static final int UNKNOWN = 0;
        private static final int PRE = 1;
        private static final int RC = 2;

        static Release parse(String value) {
            Matcher matcher = PATTERN.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(value);
            }

            int major = Integer.parseInt(matcher.group("major"));
            int minor = Integer.parseInt(matcher.group("minor"));

            String patchString = matcher.group("patch");
            int patch = patchString != null ? Integer.parseInt(patchString) : 0;

            String eaTypeString = matcher.group("eaType");
            int eaType;
            if (eaTypeString == null) {
                eaType = GA;
            } else if ("-pre".equals(eaTypeString) || " Pre-Release ".equals(eaTypeString)) {
                eaType = PRE;
            } else if ("-rc".equals(eaTypeString)) {
                eaType = RC;
            } else {
                eaType = UNKNOWN;
            }

            String eaVersionString = matcher.group("eaVersion");
            int eaVersion = eaVersionString == null ? 0 : Integer.parseInt(eaVersionString);

            return new Release(value, major, minor, patch, eaType, eaVersion);
        }

        private final int major;
        private final int minor;
        private final int patch;

        private final int eaType;
        private final int eaVersion;

        Release(String value, int major, int minor, int patch, int eaType, int eaVersion) {
            super(value);
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.eaType = eaType;
            this.eaVersion = eaVersion;
        }

        @Override
        Type getType() {
            return Type.NEW;
        }

        int compareToSnapshot(Snapshot other) {
            return 0; // TODO
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            if (other instanceof Release) {
                Release that = (Release) other;

                int c = Integer.compare(this.major, that.major);
                if (c != 0) {
                    return c;
                }

                c = Integer.compare(this.minor, that.minor);
                if (c != 0) {
                    return c;
                }

                c = Integer.compare(this.patch, that.patch);
                if (c != 0) {
                    return c;
                }

                c = Integer.compare(this.eaType, that.eaType);
                if (c != 0) {
                    return c;
                }

                return Integer.compare(this.eaVersion, that.eaVersion);
            }

            if (other instanceof Snapshot) {
                return compareToSnapshot((Snapshot) other);
            }

            if (other instanceof Special) {
                return ((Special) other).compareToRelease(this);
            }

            throw new AssertionError(other.getClass());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Release other = (Release) o;
            return major == other.major && minor == other.minor && patch == other.patch && eaType == other.eaType && eaVersion == other.eaVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, eaType, eaVersion);
        }
    }

    static final class Snapshot extends GameVersionNumber {

        private static final Pattern PATTERN = Pattern.compile("(?<year>[0-9][0-9])w(?<week>[0-9][0-9])(?<suffix>.*)");

        static Snapshot parse(String value) {
            if (value.length() != 6 || value.charAt(2) != 'w') {
                throw new IllegalArgumentException(value);
            }

            int year;
            int week;
            try {
                year = Integer.parseInt(value.substring(0, 2));
                week = Integer.parseInt(value.substring(3, 5));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(value);
            }

            char suffix = value.charAt(5);
            if (suffix < 'a' || suffix > 'z') {
                throw new IllegalArgumentException(value);
            }

            return new Snapshot(value, year, week, suffix);
        }

        private final int year;
        private final int week;
        private final char suffix;

        private Release prevRelease;
        private Release nextRelease;

        Snapshot(String value, int year, int week, char suffix) {
            super(value);
            this.year = year;
            this.week = week;
            this.suffix = suffix;
        }

        @Override
        Type getType() {
            return Type.NEW;
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            if (other instanceof Release) {
                return -((Release) other).compareToSnapshot(this);
            }

            if (other instanceof Snapshot) {
                Snapshot that = (Snapshot) other;
                int c = Integer.compare(this.year, that.year);
                if (c != 0)
                    return c;

                c = Integer.compare(this.week, that.week);
                if (c != 0)
                    return c;

                return Character.compare(this.suffix, that.suffix);
            }

            if (other instanceof Special) {
                return ((Special) other).compareToSnapshot(this);
            }

            throw new AssertionError(other.getClass());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Snapshot other = (Snapshot) o;
            return year == other.year && week == other.week && suffix == other.suffix;
        }

        @Override
        public int hashCode() {
            return Objects.hash(year, week, suffix);
        }
    }

    static final class Special extends GameVersionNumber {

        private GameVersionNumber prev;
        private GameVersionNumber next;

        Special(String value, GameVersionNumber prev, GameVersionNumber next) {
            super(value);
            this.prev = prev;
            this.next = next;
        }

        @Override
        Type getType() {
            return Type.NEW;
        }

        int compareToRelease(Release other) {
            return 0; // TODO
        }

        int compareToSnapshot(Snapshot other) {
            return 0; // TODO
        }

        @Override
        int compareToImpl(@NotNull GameVersionNumber other) {
            if (other instanceof Release) {
                return compareToRelease((Release) other);
            }

            if (other instanceof Snapshot) {
                return compareToSnapshot((Snapshot) other);
            }

            if (other instanceof Special) {
                Special that = (Special) other;

                // TODO
            }

            throw new AssertionError(other.getClass());
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Special other = (Special) o;
            return Objects.equals(this.value, other.value);
        }
    }

    static final class Database {
        private static final Snapshot[] SNAPSHOTS;

        static {
            List<GameVersionNumber> versions = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(GameVersionNumber.class.getResourceAsStream("/assets/game/versions.txt"), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null && !line.isEmpty(); ) {
                    versions.add(GameVersionNumber.asGameVersion(line));
                }
            } catch (IOException e) {
                throw new InternalError(e);
            }

            Release currentRelease = null;

            List<Snapshot> snapshots = new ArrayList<>(32);

            int n = 0;
            for (int i = 0; i < versions.size(); i++) {
                GameVersionNumber version = versions.get(i);
                if (version instanceof Snapshot) {
                    Snapshot snapshot = (Snapshot) version;

                    snapshot.nextRelease = currentRelease;
                    snapshots.add(snapshot);
                    n++;
                } else if (version instanceof Release) {
                    currentRelease = (Release) version;

                    for (int j = snapshots.size() - n; j < snapshots.size(); j++) {
                        snapshots.get(j).prevRelease = currentRelease;
                    }

                    n = 0;
                } else if (version instanceof Special) {

                } else {
                    throw new InternalError("version: " + version);
                }
            }

            for (int j = snapshots.size() - n; j < snapshots.size(); j++) {
                snapshots.get(j).prevRelease = currentRelease;
            }

            SNAPSHOTS = snapshots.toArray(new Snapshot[0]);
        }
    }
}
