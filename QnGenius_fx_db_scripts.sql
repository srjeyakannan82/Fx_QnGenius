--
-- Table structure for users
--
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'student',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for courses
--
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code VARCHAR(50) UNIQUE NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for subjects
--
CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID REFERENCES courses(id),
    subject_code VARCHAR(50) UNIQUE NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for units
--
CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID REFERENCES subjects(id),
    unit_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for course_outcomes
--
CREATE TABLE course_outcomes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID REFERENCES subjects(id),
    co_code VARCHAR(10) NOT NULL,
    co_description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (subject_id, co_code)
);

--
-- Table structure for questions
--
CREATE TABLE questions (
    question_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID REFERENCES units(id),
    co_id UUID REFERENCES course_outcomes(id),
    question_text TEXT NOT NULL,
    question_type VARCHAR(50),
    marks INTEGER,
    difficulty_level VARCHAR(50),
    importance_level VARCHAR(50),
    application_level VARCHAR(50),
    bloom_taxonomy_level VARCHAR(50),
    course_outcome VARCHAR(255),
    keywords TEXT,
    created_by UUID REFERENCES users(id),
    raw_content TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for exam_types
--
CREATE TABLE exam_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_name VARCHAR(50) UNIQUE NOT NULL
);

-- Insert common exam types
INSERT INTO exam_types (type_name) VALUES ('Internal Exam 1'), ('Internal Exam 2'), ('Internal Exam 3'), ('Semester Exam');

--
-- Table structure for blueprints
--
CREATE TABLE blueprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID REFERENCES subjects(id),
    exam_type_id UUID REFERENCES exam_types(id),
    title VARCHAR(255) NOT NULL,
    total_marks INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for blueprint_sections
--
CREATE TABLE blueprint_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blueprint_id UUID REFERENCES blueprints(id),
    section_name VARCHAR(50) NOT NULL,
    section_marks INTEGER NOT NULL,
    is_internal_choice BOOLEAN NOT NULL DEFAULT FALSE
);

--
-- Table structure for blueprint_criteria
--
CREATE TABLE blueprint_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blueprint_section_id UUID REFERENCES blueprint_sections(id),
    question_type VARCHAR(50) NOT NULL,
    number_of_questions INTEGER NOT NULL,
    marks_per_question INTEGER NOT NULL,
    difficulty_level VARCHAR(50) NOT NULL,
    bloom_taxonomy_level VARCHAR(50) NOT NULL
);


-- Table to store user groups
CREATE TABLE user_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table to link users to groups (a user can be in multiple groups)
CREATE TABLE user_group_members (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES user_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

-- Table to log user login events for auditing
CREATE TABLE login_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    login_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    success BOOLEAN NOT NULL
);